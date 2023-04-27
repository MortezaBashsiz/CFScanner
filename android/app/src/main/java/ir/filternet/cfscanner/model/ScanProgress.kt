package ir.filternet.cfscanner.model

import kotlinx.serialization.Serializable

@Serializable
data class ScanProgress(
    val totalConnectionCount:Int = 0 ,
    val checkConnectionCount:Int = 0,
    val successConnectionCount:Int = 0,
)