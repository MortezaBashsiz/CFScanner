package ir.filternet.cfscanner.scanner.v2ray

import android.text.TextUtils
import com.google.gson.Gson
import ir.filternet.cfscanner.utils.AppConfig.TAG_AGENT
import ir.filternet.cfscanner.utils.AppConfig.TAG_BLOCKED
import ir.filternet.cfscanner.utils.AppConfig.TAG_DIRECT
import ir.filternet.cfscanner.utils.encode
import ir.filternet.cfscanner.utils.getIpv6Address
import ir.filternet.cfscanner.utils.isIpv6Address
import ir.filternet.cfscanner.utils.removeWhiteSpace
import ir.filternet.cfscanner.utils.urlEncode

data class ServerConfig(
    val configVersion: Int = 3,
    val configType: EConfigType,
    var subscriptionId: String = "",
    val addedTime: Long = System.currentTimeMillis(),
    var remarks: String = "",
    val outboundBean: V2rayConfig.OutboundBean? = null,
    var fullConfig: V2rayConfig? = null
) {
    companion object {
        fun create(configType: EConfigType): ServerConfig {
            when(configType) {
                EConfigType.VMESS, EConfigType.VLESS ->
                    return ServerConfig(
                            configType = configType,
                            outboundBean = V2rayConfig.OutboundBean(
                                    protocol = configType.name.lowercase(),
                                    settings = V2rayConfig.OutboundBean.OutSettingsBean(
                                            vnext = listOf(
                                                V2rayConfig.OutboundBean.OutSettingsBean.VnextBean(
                                                    users = listOf(V2rayConfig.OutboundBean.OutSettingsBean.VnextBean.UsersBean()))
                                            )),
                                    streamSettings = V2rayConfig.OutboundBean.StreamSettingsBean()
                            )
                    )
                EConfigType.CUSTOM, EConfigType.WIREGUARD ->
                    return ServerConfig(configType = configType)
                EConfigType.SHADOWSOCKS, EConfigType.SOCKS, EConfigType.TROJAN ->
                    return ServerConfig(
                            configType = configType,
                            outboundBean = V2rayConfig.OutboundBean(
                                    protocol = configType.name.lowercase(),
                                    settings = V2rayConfig.OutboundBean.OutSettingsBean(
                                            servers = listOf(V2rayConfig.OutboundBean.OutSettingsBean.ServersBean())),
                                    streamSettings = V2rayConfig.OutboundBean.StreamSettingsBean()
                            )
                    )
            }
        }
    }


    fun getProxyOutbound(): V2rayConfig.OutboundBean? {
        if (configType != EConfigType.CUSTOM) {
            return outboundBean
        }
        return fullConfig?.getProxyOutbound()
    }

    fun getAllOutboundTags(): MutableList<String> {
        if (configType != EConfigType.CUSTOM) {
            return mutableListOf(TAG_AGENT, TAG_DIRECT, TAG_BLOCKED)
        }
        fullConfig?.let { config ->
            return config.outbounds.map { it.tag }.toMutableList()
        }
        return mutableListOf()
    }

    fun getV2rayPointDomainAndPort(): String {
        val address = getProxyOutbound()?.getServerAddress().orEmpty()
        val port = getProxyOutbound()?.getServerPort()
        return if (isIpv6Address(address)) {
            String.format("[%s]:%s", address, port)
        } else {
            String.format("%s:%s", address, port)
        }
    }


    fun getShareableConfig(): String {
        try {
            val config = this

            val outbound = getProxyOutbound() ?: return ""
            val streamSetting = outbound.streamSettings ?: return ""
            return configType.protocolScheme + when (config.configType) {
                EConfigType.VMESS -> {
                    val vmessQRCode = VmessQRCode()
                    vmessQRCode.v = "2"
                    vmessQRCode.ps = config.remarks
                    vmessQRCode.add = outbound.getServerAddress().orEmpty()
                    vmessQRCode.port = outbound.getServerPort().toString()
                    vmessQRCode.id = outbound.getPassword().orEmpty()
                    vmessQRCode.aid = outbound.settings?.vnext?.get(0)?.users?.get(0)?.alterId.toString()
                    vmessQRCode.scy = outbound.settings?.vnext?.get(0)?.users?.get(0)?.security.toString()
                    vmessQRCode.net = streamSetting.network
                    vmessQRCode.tls = streamSetting.security
                    vmessQRCode.sni = streamSetting.tlsSettings?.serverName.orEmpty()
                    vmessQRCode.alpn = removeWhiteSpace(streamSetting.tlsSettings?.alpn?.joinToString()).orEmpty()
                    vmessQRCode.fp = streamSetting.tlsSettings?.fingerprint.orEmpty()
                    outbound.getTransportSettingDetails()?.let { transportDetails ->
                        vmessQRCode.type = transportDetails[0]
                        vmessQRCode.host = transportDetails[1]
                        vmessQRCode.path = transportDetails[2]
                    }
                    val json = Gson().toJson(vmessQRCode)
                    encode(json)
                }
                EConfigType.CUSTOM, EConfigType.WIREGUARD -> ""
                EConfigType.SHADOWSOCKS -> {
                    val remark = "#" + urlEncode(config.remarks)
                    val pw = encode("${outbound.getSecurityEncryption()}:${outbound.getPassword()}")
                    val url = String.format("%s@%s:%s",
                        pw,
                        getIpv6Address(outbound.getServerAddress()!!),
                        outbound.getServerPort())
                    url + remark
                }
                EConfigType.SOCKS -> {
                    val remark = "#" + urlEncode(config.remarks)
                    val pw = encode("${outbound.settings?.servers?.get(0)?.users?.get(0)?.user}:${outbound.getPassword()}")
                    val url = String.format("%s@%s:%s",
                        pw,
                        getIpv6Address(outbound.getServerAddress()!!),
                        outbound.getServerPort())
                    url + remark
                }
                EConfigType.VLESS,
                EConfigType.TROJAN -> {
                    val remark = "#" + urlEncode(config.remarks)

                    val dicQuery = HashMap<String, String>()
                    if (config.configType == EConfigType.VLESS) {
                        outbound.settings?.vnext?.get(0)?.users?.get(0)?.flow?.let {
                            if (!TextUtils.isEmpty(it)) {
                                dicQuery["flow"] = it
                            }
                        }
                        dicQuery["encryption"] =
                            if (outbound.getSecurityEncryption().isNullOrEmpty()) "none"
                            else outbound.getSecurityEncryption().orEmpty()
                    } else if (configType == EConfigType.TROJAN) {
                        outboundBean?.settings?.servers?.get(0)?.flow?.let {
                            if (!TextUtils.isEmpty(it)) {
                                dicQuery["flow"] = it
                            }
                        }
                    }

                    dicQuery["security"] = streamSetting.security.ifEmpty { "none" }
//                    (streamSetting.tlsSettings?: streamSetting.realitySettings)?.let { tlsSetting ->
//                        if (!TextUtils.isEmpty(tlsSetting.serverName)) {
//                            dicQuery["sni"] = tlsSetting.serverName
//                        }
//                        if (!tlsSetting.alpn.isNullOrEmpty() && tlsSetting.alpn.isNotEmpty()) {
//                            dicQuery["alpn"] = removeWhiteSpace(tlsSetting.alpn.joinToString()).orEmpty()
//                        }
//                        if (!TextUtils.isEmpty(tlsSetting.fingerprint)) {
//                            dicQuery["fp"] = tlsSetting.fingerprint!!
//                        }
//                        if (!TextUtils.isEmpty(tlsSetting.publicKey)) {
//                            dicQuery["pbk"] = tlsSetting.publicKey!!
//                        }
//                        if (!TextUtils.isEmpty(tlsSetting.shortId)) {
//                            dicQuery["sid"] = tlsSetting.shortId!!
//                        }
//                        if (!TextUtils.isEmpty(tlsSetting.spiderX)) {
//                            dicQuery["spx"] = urlEncode(tlsSetting.spiderX!!)
//                        }
//                    }
                    dicQuery["type"] = streamSetting.network.ifEmpty { V2rayConfig.DEFAULT_NETWORK }

                    outbound.getTransportSettingDetails()?.let { transportDetails ->
                        when (streamSetting.network) {
                            "tcp" -> {
                                dicQuery["headerType"] = transportDetails[0].ifEmpty { "none" }
                                if (!TextUtils.isEmpty(transportDetails[1])) {
                                    dicQuery["host"] = urlEncode(transportDetails[1])
                                }
                            }
                            "kcp" -> {
                                dicQuery["headerType"] = transportDetails[0].ifEmpty { "none" }
                                if (!TextUtils.isEmpty(transportDetails[2])) {
                                    dicQuery["seed"] = urlEncode(transportDetails[2])
                                }
                            }
                            "ws" -> {
                                if (!TextUtils.isEmpty(transportDetails[1])) {
                                    dicQuery["host"] = urlEncode(transportDetails[1])
                                }
                                if (!TextUtils.isEmpty(transportDetails[2])) {
                                    dicQuery["path"] = urlEncode(transportDetails[2])
                                }
                            }
                            "http", "h2" -> {
                                dicQuery["type"] = "http"
                                if (!TextUtils.isEmpty(transportDetails[1])) {
                                    dicQuery["host"] = urlEncode(transportDetails[1])
                                }
                                if (!TextUtils.isEmpty(transportDetails[2])) {
                                    dicQuery["path"] = urlEncode(transportDetails[2])
                                }
                            }
                            "quic" -> {
                                dicQuery["headerType"] = transportDetails[0].ifEmpty { "none" }
                                dicQuery["quicSecurity"] = urlEncode(transportDetails[1])
                                dicQuery["key"] = urlEncode(transportDetails[2])
                            }
                            "grpc" -> {
                                dicQuery["mode"] = transportDetails[0]
                                dicQuery["serviceName"] = transportDetails[2]
                            }
                        }
                    }
                    val query = "?" + dicQuery.toList().joinToString(
                        separator = "&",
                        transform = { it.first + "=" + it.second })

                    val url = String.format("%s@%s:%s",
                        outbound.getPassword(),
                        getIpv6Address(outbound.getServerAddress()!!),
                        outbound.getServerPort())
                    url + query + remark
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }


}
