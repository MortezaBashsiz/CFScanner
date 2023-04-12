package ir.filternet.cfscanner.scanner.v2ray

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.google.gson.*
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.filternet.cfscanner.model.Settings
import ir.filternet.cfscanner.utils.*
import java.net.URI
import javax.inject.Inject

class V2rayConfigUtil @Inject constructor(@ApplicationContext private val context: Context) {

    fun createV2rayConfig(text: String): V2rayConfig? {
        return createServerConfig(text)?.fullConfig
    }

    fun createV2rayConfigString(text: String): String? {
        return createServerConfig(text)?.fullConfig?.toPrettyPrinting()
    }

    fun createServerConfig(text: String): ServerConfig? {
        val settings = Settings()
        val v2rayConfig = getV2raySampleConfig(context) ?: return null
        val serverConfig = getV2rayServerConfig(text) ?: return null
        v2rayConfig.apply {
            log = getV2rayLogConfig("warning")
            inbounds = getV2rayInboundConfig(settings)
            outbounds = getV2rayOutboundConfig(serverConfig, settings)
            routing = getV2rayRoutingConfig(settings)
            fakedns = getV2rayFakeDnsConfig(settings)
            dns = getV2rayDnsConfig(settings,this)

            if(settings.localDnsEnabled){
                setCustomLocalDns(settings,this)
            }

            if(settings.speedEnabled){
                stats = null
                policy = null
            }
        }
        return serverConfig.apply { fullConfig = v2rayConfig }
    }

    private fun getV2raySampleConfig(context: Context): V2rayConfig? {
        val assets = readTextFromAssets(context, "v2ray_config.json")
        if (TextUtils.isEmpty(assets)) {
            return null
        }
        return Gson().fromJson(assets, V2rayConfig::class.java)
    }

    private fun getV2rayInboundConfig(settings: Settings): ArrayList<V2rayConfig.InboundBean> {
        return arrayListOf(
            V2rayConfig.InboundBean(
                tag = "socks",
                protocol = "socks",
                port = settings.socksPort,
                listen = settings.socketAddress,
                settings = V2rayConfig.InboundBean.InSettingsBean("noauth", udp = true, userLevel = 8),
                sniffing = V2rayConfig.InboundBean.SniffingBean(
                    enabled = true,
                    destOverride = arrayListOf("http", "tls").apply {
                        if (!settings.sniffEnabled) {
                            clear()
                        }

                        if (settings.fakeDnsEnabled) {
                            add("fakedns")
                        }
                    }
                )
            ),
            V2rayConfig.InboundBean(
                tag = "http",
                protocol = "http",
                port = settings.httpPort,
                listen = settings.httpAddress,
                settings = V2rayConfig.InboundBean.InSettingsBean("noauth", udp = true, userLevel = 8),
                sniffing = V2rayConfig.InboundBean.SniffingBean(
                    enabled = true,
                    destOverride = arrayListOf("http", "tls").apply {
                        if (!settings.sniffEnabled) {
                            clear()
                        }

                        if (settings.fakeDnsEnabled) {
                            add("fakedns")
                        }
                    }
                )
            )
        )
    }

    private fun getV2rayLogConfig(level: String = "warning"): V2rayConfig.LogBean {
        return V2rayConfig.LogBean(
            access = "",
            error = "",
            loglevel = level
        )
    }

    private fun getV2rayOutboundConfig(serverConfig: ServerConfig?, settings: Settings): ArrayList<V2rayConfig.OutboundBean> {
        val arrayList = ArrayList<V2rayConfig.OutboundBean>()

        if (serverConfig?.outboundBean != null) {
            arrayList.add(serverConfig.outboundBean)
            return arrayList
        }

        for (outbound in arrayList) {
            outbound.applyTcpHeaderAdditionConfig()
            if (settings.fakeDnsEnabled && outbound.protocol == "freedom") {
                outbound.settings?.domainStrategy = "UseIP"
            }
        }


        return arrayList
    }

    private fun getV2rayRoutingConfig(settings: Settings): V2rayConfig.RoutingBean {
        val route = V2rayConfig.RoutingBean(
            settings.domainStrategy,
            settings.domainMatcher,
            arrayListOf<V2rayConfig.RoutingBean.RulesBean>().apply {
                addAll(getRoutingUserRule(settings.routingAgent, AppConfig.PREF_V2RAY_ROUTING_AGENT))
                addAll(getRoutingUserRule(settings.routingDirect, AppConfig.PREF_V2RAY_ROUTING_DIRECT))
                addAll(getRoutingUserRule(settings.routingBlock, AppConfig.PREF_V2RAY_ROUTING_BLOCKED))
                addAll(gerRoutingUser(settings.routingMode))
            },
            arrayListOf()
        )
        return route
    }

    private fun setCustomLocalDns(settings: Settings, v2rayConfig: V2rayConfig): Boolean {
        try {
            if (settings.fakeDnsEnabled) {
                val geositeCn = arrayListOf("geosite:cn")
                val proxyDomain = userRule2Domian(settings.routingAgent)
                val directDomain = userRule2Domian(settings.routingDirect)
                v2rayConfig.dns.servers?.add(0, V2rayConfig.DnsBean.ServersBean(address = "fakedns", domains = geositeCn.plus(proxyDomain).plus(directDomain)))
            }


            val remoteDns = getRemoteDnsServers()
            if (v2rayConfig.inbounds.none { e -> e.protocol == "dokodemo-door" && e.tag == "dns-in" }) {
                val dnsInboundSettings = V2rayConfig.InboundBean.InSettingsBean(
                    address = if (isPureIpAddress(remoteDns.first())) remoteDns.first() else "1.1.1.1",
                    port = 53,
                    network = "tcp,udp"
                )

                v2rayConfig.inbounds.add(
                    V2rayConfig.InboundBean(
                        tag = "dns-in",
                        port = settings.localDnsPort,
                        listen = "127.0.0.1",
                        protocol = "dokodemo-door",
                        settings = dnsInboundSettings,
                        sniffing = null
                    )
                )
            }

            if (v2rayConfig.outbounds.none { e -> e.protocol == "dns" && e.tag == "dns-out" }) {
                v2rayConfig.outbounds.add(
                    V2rayConfig.OutboundBean(
                        protocol = "dns",
                        tag = "dns-out",
                        settings = null,
                        streamSettings = null,
                        mux = null
                    )
                )
            }


            v2rayConfig.routing.rules.add(
                0, V2rayConfig.RoutingBean.RulesBean(
                    type = "field",
                    inboundTag = arrayListOf("dns-in"),
                    outboundTag = "dns-out",
                    domain = null
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    private fun getV2rayDnsConfig(settings: Settings, config: V2rayConfig): V2rayConfig.DnsBean {
        try {
            val hosts = mutableMapOf<String, String>()
            val servers = ArrayList<Any>()
            val remoteDns = getRemoteDnsServers()
            val proxyDomain = userRule2Domian(settings.routingAgent)

            remoteDns.forEach {
                servers.add(it)
            }

            if (proxyDomain.size > 0) {
                servers.add(V2rayConfig.DnsBean.ServersBean(remoteDns.first(), 53, proxyDomain, null))
            }

            // domestic DNS
            val directDomain = userRule2Domian(settings.routingDirect)
            val routingMode = settings.routingMode
            if (directDomain.size > 0 || routingMode == ERoutingMode.BYPASS_MAINLAND.value || routingMode == ERoutingMode.BYPASS_LAN_MAINLAND.value) {
                val domesticDns = getDomesticDnsServers(settings.domesticDns)
                val geositeCn = arrayListOf("geosite:cn")
                val geoipCn = arrayListOf("geoip:cn")
                if (directDomain.size > 0) {
                    servers.add(V2rayConfig.DnsBean.ServersBean(domesticDns.first(), 53, directDomain, geoipCn))
                }
                if (routingMode == ERoutingMode.BYPASS_MAINLAND.value || routingMode == ERoutingMode.BYPASS_LAN_MAINLAND.value) {
                    servers.add(V2rayConfig.DnsBean.ServersBean(domesticDns.first(), 53, geositeCn, geoipCn))
                }
                if (isPureIpAddress(domesticDns.first())) {
                    config.routing.rules.add(
                        0, V2rayConfig.RoutingBean.RulesBean(
                            type = "field",
                            outboundTag = AppConfig.TAG_DIRECT,
                            port = "53",
                            ip = arrayListOf(domesticDns.first()),
                            domain = null
                        )
                    )
                }
            }

            val blkDomain = userRule2Domian(settings.routingBlock)
            if (blkDomain.size > 0) {
                hosts.putAll(blkDomain.map { it to "127.0.0.1" })
            }

            // hardcode googleapi rule to fix play store problems
            hosts["domain:googleapis.cn"] = "googleapis.com"

            // DNS routing
            if (isPureIpAddress(remoteDns.first())) {
                config.routing.rules.add(
                    0, V2rayConfig.RoutingBean.RulesBean(
                        type = "field",
                        outboundTag = AppConfig.TAG_AGENT,
                        port = "53",
                        ip = arrayListOf(remoteDns.first()),
                        domain = null
                    )
                )
            }

            return V2rayConfig.DnsBean(
                servers = servers,
                hosts = hosts
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return V2rayConfig.DnsBean()
        }
    }

    private fun getV2rayFakeDnsConfig(settings: Settings): List<V2rayConfig.FakednsBean> {
        return if (settings.fakeDnsEnabled) {
            listOf(V2rayConfig.FakednsBean())
        } else
            listOf()
    }

    private fun getRoutingGeoRules(ipOrDomain: String, code: String, tag: String): ArrayList<V2rayConfig.RoutingBean.RulesBean> {
        val rules = ArrayList<V2rayConfig.RoutingBean.RulesBean>()
        try {
            if (!TextUtils.isEmpty(code)) {
                //IP
                if (ipOrDomain == "ip" || ipOrDomain == "") {
                    val rulesIP = V2rayConfig.RoutingBean.RulesBean()
                    rulesIP.type = "field"
                    rulesIP.outboundTag = tag
                    rulesIP.ip = ArrayList()
                    rulesIP.ip?.add("geoip:$code")
                    rules.add(rulesIP)
                }

                if (ipOrDomain == "domain" || ipOrDomain == "") {
                    //Domain
                    val rulesDomain = V2rayConfig.RoutingBean.RulesBean()
                    rulesDomain.type = "field"
                    rulesDomain.outboundTag = tag
                    rulesDomain.domain = ArrayList()
                    rulesDomain.domain?.add("geosite:$code")
                    rules.add(rulesDomain)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return rules
    }

    private fun getRoutingUserRule(userRule: String, tag: String): ArrayList<V2rayConfig.RoutingBean.RulesBean> {
        val rules = ArrayList<V2rayConfig.RoutingBean.RulesBean>()
        try {
            if (!TextUtils.isEmpty(userRule)) {
                //Domain
                val rulesDomain = V2rayConfig.RoutingBean.RulesBean()
                rulesDomain.type = "field"
                rulesDomain.outboundTag = tag
                rulesDomain.domain = ArrayList()

                //IP
                val rulesIP = V2rayConfig.RoutingBean.RulesBean()
                rulesIP.type = "field"
                rulesIP.outboundTag = tag
                rulesIP.ip = ArrayList()

                userRule.split(",").map { it.trim() }.forEach {
                    if (isIpAddress(it) || it.startsWith("geoip:")) {
                        rulesIP.ip?.add(it)
                    } else if (it.isNotEmpty()) {
                        rulesDomain.domain?.add(it)
                    }
                }
                if (rulesDomain.domain?.size!! > 0) {
                    rules.add(rulesDomain)
                }
                if (rulesIP.ip?.size!! > 0) {
                    rules.add(rulesIP)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return rules
    }

    private fun gerRoutingUser(routingMode: String): ArrayList<V2rayConfig.RoutingBean.RulesBean> {
        val rules = ArrayList<V2rayConfig.RoutingBean.RulesBean>()
        val googleapisRoute = V2rayConfig.RoutingBean.RulesBean(
            type = "field",
            outboundTag = AppConfig.TAG_AGENT,
            domain = arrayListOf("domain:googleapis.cn")
        )
        when (routingMode) {
            ERoutingMode.BYPASS_LAN.value -> {
                rules.addAll(getRoutingGeoRules("ip", "private", AppConfig.TAG_DIRECT))
            }
            ERoutingMode.BYPASS_MAINLAND.value -> {
                rules.addAll(getRoutingGeoRules("", "cn", AppConfig.TAG_DIRECT))
                rules.add(0, googleapisRoute)
            }
            ERoutingMode.BYPASS_LAN_MAINLAND.value -> {
                getRoutingGeoRules("ip", "private", AppConfig.TAG_DIRECT)
                getRoutingGeoRules("", "cn", AppConfig.TAG_DIRECT)
                rules.add(0, googleapisRoute)
            }
            ERoutingMode.GLOBAL_DIRECT.value -> {
                val globalDirect = V2rayConfig.RoutingBean.RulesBean(
                    type = "field",
                    outboundTag = AppConfig.TAG_DIRECT,
                    port = "0-65535"
                )
                rules.add(globalDirect)
            }
        }
        return rules
    }

    private fun userRule2Domian(userRule: String): ArrayList<String> {
        val domain = ArrayList<String>()
        userRule.split(",").map { it.trim() }.forEach {
            if (it.startsWith("geosite:") || it.startsWith("domain:")) {
                domain.add(it)
            }
        }
        return domain
    }

    private fun V2rayConfig.OutboundBean.applyTcpHeaderAdditionConfig() {
        try {
            if (streamSettings?.network == V2rayConfig.DEFAULT_NETWORK
                && streamSettings?.tcpSettings?.header?.type == V2rayConfig.HTTP
            ) {
                val path = streamSettings?.tcpSettings?.header?.request?.path
                val host = streamSettings?.tcpSettings?.header?.request?.headers?.Host

                val requestString: String by lazy {
                    """{"version":"1.1","method":"GET","headers":{"User-Agent":["Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36","Mozilla/5.0 (iPhone; CPU iPhone OS 10_0_2 like Mac OS X) AppleWebKit/601.1 (KHTML, like Gecko) CriOS/53.0.2785.109 Mobile/14A456 Safari/601.1.46"],"Accept-Encoding":["gzip, deflate"],"ConnectionEntity":["keep-alive"],"Pragma":"no-cache"}}"""
                }
                streamSettings?.tcpSettings?.header?.request = Gson().fromJson(
                    requestString,
                    V2rayConfig.OutboundBean.StreamSettingsBean.TcpSettingsBean.HeaderBean.RequestBean::class.java
                )
                streamSettings?.tcpSettings?.header?.request?.path =
                    if (path.isNullOrEmpty()) {
                        listOf("/")
                    } else {
                        path
                    }
                streamSettings?.tcpSettings?.header?.request?.headers?.Host = host!!
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getV2rayServerConfig(str: String?): ServerConfig? {
        try {

            if (str == null || TextUtils.isEmpty(str)) {
                return null
            }

            var config: ServerConfig? = null
            val allowInsecure = false
            if (str.startsWith(EConfigType.VMESS.protocolScheme)) {
                config = ServerConfig.create(EConfigType.VMESS)
                val streamSetting = config.outboundBean?.streamSettings ?: return null


                if (!tryParseNewVmess(str, config, allowInsecure)) {
                    if (str.indexOf("?") > 0) {
                        if (!tryResolveVmess4Kitsunebi(str, config)) {
                            return null //incorrect Protocol
                        }
                    } else {
                        var result = str.replace(EConfigType.VMESS.protocolScheme, "")
                        result = decode(result)
                        if (TextUtils.isEmpty(result)) {
                            return null // decode failed
                        }

                        val vmessQRCode = Gson().fromJson(result, VmessQRCode::class.java)
                        // Although VmessQRCode fields are non null, looks like Gson may still create null fields
                        if (TextUtils.isEmpty(vmessQRCode.add)
                            || TextUtils.isEmpty(vmessQRCode.port)
                            || TextUtils.isEmpty(vmessQRCode.id)
                            || TextUtils.isEmpty(vmessQRCode.net)
                        ) {
                            return null// incoorect protocol
                        }

                        config.remarks = vmessQRCode.ps
                        config.outboundBean?.settings?.vnext?.get(0)?.let { vnext ->
                            vnext.address = vmessQRCode.add
                            vnext.port = parseInt(vmessQRCode.port)
                            vnext.users[0].id = vmessQRCode.id
                            vnext.users[0].security = if (TextUtils.isEmpty(vmessQRCode.scy)) V2rayConfig.DEFAULT_SECURITY else vmessQRCode.scy
                            vnext.users[0].alterId = parseInt(vmessQRCode.aid)
                        }
                        val sni = streamSetting.populateTransportSettings(
                            vmessQRCode.net, vmessQRCode.type, vmessQRCode.host,
                            vmessQRCode.path, vmessQRCode.path, vmessQRCode.host, vmessQRCode.path, vmessQRCode.type, vmessQRCode.path
                        )

                        val fingerprint = vmessQRCode.fp ?: streamSetting.tlsSettings?.fingerprint
                        streamSetting.populateTlsSettings(
                            vmessQRCode.tls, allowInsecure,
                            if (TextUtils.isEmpty(vmessQRCode.sni)) sni else vmessQRCode.sni, fingerprint, vmessQRCode.alpn
                        )
                    }
                }
            } else if (str.startsWith(EConfigType.SHADOWSOCKS.protocolScheme)) {
                config = ServerConfig.create(EConfigType.SHADOWSOCKS)
                if (!tryResolveResolveSip002(str, config)) {
                    var result = str.replace(EConfigType.SHADOWSOCKS.protocolScheme, "")
                    val indexSplit = result.indexOf("#")
                    if (indexSplit > 0) {
                        try {
                            config.remarks = urlDecode(result.substring(indexSplit + 1, result.length))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        result = result.substring(0, indexSplit)
                    }

                    //part decode
                    val indexS = result.indexOf("@")
                    result = if (indexS > 0) {
                        decode(result.substring(0, indexS)) + result.substring(indexS, result.length)
                    } else {
                        decode(result)
                    }

                    val legacyPattern = "^(.+?):(.*)@(.+?):(\\d+?)/?$".toRegex()
                    val match = legacyPattern.matchEntire(result) ?: return null//incoorect protocol

                    config.outboundBean?.settings?.servers?.get(0)?.let { server ->
                        server.address = match.groupValues[3].removeSurrounding("[", "]")
                        server.port = match.groupValues[4].toInt()
                        server.password = match.groupValues[2]
                        server.method = match.groupValues[1].lowercase()
                    }
                }
            } else if (str.startsWith(EConfigType.SOCKS.protocolScheme)) {
                var result = str.replace(EConfigType.SOCKS.protocolScheme, "")
                val indexSplit = result.indexOf("#")
                config = ServerConfig.create(EConfigType.SOCKS)
                if (indexSplit > 0) {
                    try {
                        config.remarks = urlDecode(result.substring(indexSplit + 1, result.length))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    result = result.substring(0, indexSplit)
                }

                //part decode
                val indexS = result.indexOf("@")
                if (indexS > 0) {
                    result = decode(result.substring(0, indexS)) + result.substring(indexS, result.length)
                } else {
                    result = decode(result)
                }

                val legacyPattern = "^(.*):(.*)@(.+?):(\\d+?)$".toRegex()
                val match = legacyPattern.matchEntire(result) ?: return null // incoorect protocol

                config.outboundBean?.settings?.servers?.get(0)?.let { server ->
                    server.address = match.groupValues[3].removeSurrounding("[", "]")
                    server.port = match.groupValues[4].toInt()
                    val socksUsersBean = V2rayConfig.OutboundBean.OutSettingsBean.ServersBean.SocksUsersBean()
                    socksUsersBean.user = match.groupValues[1].lowercase()
                    socksUsersBean.pass = match.groupValues[2]
                    server.users = listOf(socksUsersBean)
                }
            } else if (str.startsWith(EConfigType.TROJAN.protocolScheme)) {
                val uri = URI(fixIllegalUrl(str))
                config = ServerConfig.create(EConfigType.TROJAN)
                config.remarks = urlDecode(uri.fragment ?: "")

                var flow = ""
                var fingerprint = config.outboundBean?.streamSettings?.tlsSettings?.fingerprint
                if (uri.rawQuery != null) {
                    val queryParam = uri.rawQuery.split("&")
                        .associate { it.split("=").let { (k, v) -> k to urlDecode(v) } }

                    val sni = config.outboundBean?.streamSettings?.populateTransportSettings(
                        queryParam["type"] ?: "tcp", queryParam["headerType"],
                        queryParam["host"], queryParam["path"], queryParam["seed"], queryParam["quicSecurity"], queryParam["key"],
                        queryParam["mode"], queryParam["serviceName"]
                    )
                    fingerprint = queryParam["fp"] ?: ""
                    config.outboundBean?.streamSettings?.populateTlsSettings(queryParam["security"] ?: V2rayConfig.TLS, allowInsecure, queryParam["sni"] ?: sni!!, fingerprint, queryParam["alpn"])
                    flow = queryParam["flow"] ?: ""
                } else {

                    config.outboundBean?.streamSettings?.populateTlsSettings(V2rayConfig.TLS, allowInsecure, "", fingerprint, null)
                }

                config.outboundBean?.settings?.servers?.get(0)?.let { server ->
                    server.address = uri.idnHost
                    server.port = uri.port
                    server.password = uri.userInfo
                    server.flow = flow
                }
            } else if (str.startsWith(EConfigType.VLESS.protocolScheme)) {
                val uri = URI(fixIllegalUrl(str))
                val queryParam = uri.rawQuery.split("&")
                    .associate { it.split("=").let { (k, v) -> k to urlDecode(v) } }
                config = ServerConfig.create(EConfigType.VLESS)
                val streamSetting = config.outboundBean?.streamSettings ?: return null
                var fingerprint = streamSetting.tlsSettings?.fingerprint

                config.remarks = urlDecode(uri.fragment ?: "")
                config.outboundBean?.settings?.vnext?.get(0)?.let { vnext ->
                    vnext.address = uri.idnHost
                    vnext.port = uri.port
                    vnext.users[0].id = uri.userInfo
                    vnext.users[0].encryption = queryParam["encryption"] ?: "none"
                    vnext.users[0].flow = queryParam["flow"] ?: ""
                }

                val sni = streamSetting.populateTransportSettings(
                    queryParam["type"] ?: "tcp", queryParam["headerType"],
                    queryParam["host"], queryParam["path"], queryParam["seed"], queryParam["quicSecurity"], queryParam["key"],
                    queryParam["mode"], queryParam["serviceName"]
                )
                fingerprint = queryParam["fp"] ?: ""
                streamSetting.populateTlsSettings(queryParam["security"] ?: "", allowInsecure, queryParam["sni"] ?: sni, fingerprint, queryParam["alpn"])
            }
            if (config == null) {
                return null // incorrect protocol
            }

            return config
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun tryParseNewVmess(uriString: String, config: ServerConfig, allowInsecure: Boolean): Boolean {
        return runCatching {
            val uri = URI(uriString)
            check(uri.scheme == "vmess")
            val (_, protocol, tlsStr, uuid, alterId) =
                Regex("(tcp|http|ws|kcp|quic|grpc)(\\+tls)?:([0-9a-z]{8}-[0-9a-z]{4}-[0-9a-z]{4}-[0-9a-z]{4}-[0-9a-z]{12})")
                    .matchEntire(uri.userInfo)?.groupValues
                    ?: error("parse user info fail.")
            val tls = tlsStr.isNotBlank()
            val queryParam = uri.rawQuery.split("&")
                .associate { it.split("=").let { (k, v) -> k to urlDecode(v) } }

            val streamSetting = config.outboundBean?.streamSettings ?: return false
            config.remarks = urlDecode(uri.fragment ?: "")
            config.outboundBean.settings?.vnext?.get(0)?.let { vnext ->
                vnext.address = uri.idnHost
                vnext.port = uri.port
                vnext.users[0].id = uuid
                vnext.users[0].security = V2rayConfig.DEFAULT_SECURITY
                vnext.users[0].alterId = alterId.toInt()
            }
            var fingerprint = streamSetting.tlsSettings?.fingerprint
            val sni = streamSetting.populateTransportSettings(protocol, queryParam["type"],
                queryParam["host"]?.split("|")?.get(0) ?: "",
                queryParam["path"]?.takeIf { it.trim() != "/" } ?: "", queryParam["seed"], queryParam["security"],
                queryParam["key"], queryParam["mode"], queryParam["serviceName"])
            streamSetting.populateTlsSettings(if (tls) V2rayConfig.TLS else "", allowInsecure, sni, fingerprint, null)
            true
        }.getOrElse { false }
    }

    private fun tryResolveVmess4Kitsunebi(server: String, config: ServerConfig): Boolean {

        var result = server.replace(EConfigType.VMESS.protocolScheme, "")
        val indexSplit = result.indexOf("?")
        if (indexSplit > 0) {
            result = result.substring(0, indexSplit)
        }
        result = decode(result)

        val arr1 = result.split('@')
        if (arr1.count() != 2) {
            return false
        }
        val arr21 = arr1[0].split(':')
        val arr22 = arr1[1].split(':')
        if (arr21.count() != 2) {
            return false
        }

        config.remarks = "Alien"
        config.outboundBean?.settings?.vnext?.get(0)?.let { vnext ->
            vnext.address = arr22[0]
            vnext.port = parseInt(arr22[1])
            vnext.users[0].id = arr21[1]
            vnext.users[0].security = arr21[0]
            vnext.users[0].alterId = 0
        }
        return true
    }

    private fun tryResolveResolveSip002(str: String, config: ServerConfig): Boolean {
        try {
            val uri = URI(fixIllegalUrl(str))
            config.remarks = urlDecode(uri.fragment ?: "")

            val method: String
            val password: String
            if (uri.userInfo.contains(":")) {
                val arrUserInfo = uri.userInfo.split(":").map { it.trim() }
                if (arrUserInfo.count() != 2) {
                    return false
                }
                method = arrUserInfo[0]
                password = urlDecode(arrUserInfo[1])
            } else {
                val base64Decode = decode(uri.userInfo)
                val arrUserInfo = base64Decode.split(":").map { it.trim() }
                if (arrUserInfo.count() < 2) {
                    return false
                }
                method = arrUserInfo[0]
                password = base64Decode.substringAfter(":")
            }

            config.outboundBean?.settings?.servers?.get(0)?.let { server ->
                server.address = uri.idnHost
                server.port = uri.port
                server.password = password
                server.method = method
            }
            return true
        } catch (e: Exception) {
            Log.d(AppConfig.ANG_PACKAGE, e.toString())
            return false
        }
    }

}
