package ir.filternet.cfscanner.model

import ir.filternet.cfscanner.utils.AppConfig

data class Settings(
    val proxyMode: Boolean = false,
    val socketAddress: String = "127.0.0.1",
    val socksPort: Int = 0,
    val httpAddress: String = "127.0.0.1",
    val httpPort: Int = 0,
    val logLevel: String = "warning",
    val domainStrategy: String = "IPIfNonMatch",
    val domainMatcher: String = "mph",
    val routingMode: String = "0",
    val routingAgent: String = "",
    val routingDirect: String = "",
    val routingBlock: String = "",
    val fakeDnsEnabled: Boolean = false,
    val localDnsPort: Int = AppConfig.PORT_LOCAL_DNS.toInt(),
    val localDnsEnabled: Boolean = false,
    val sniffEnabled: Boolean = true,
    val vpnDns: String = "",
    val remoteDns: String = "",
    val domesticDns:String = AppConfig.DNS_DIRECT,
    val speedEnabled:Boolean = false,
)