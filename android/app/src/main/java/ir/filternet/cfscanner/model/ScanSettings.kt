package ir.filternet.cfscanner.model
import ir.filternet.cfscanner.BuildConfig

data class ScanSettings(
    val worker: Float = 4.99f,
    val speedTestSize: Float = 300f,
    val fronting: String = BuildConfig.FrontingAddress,
)
