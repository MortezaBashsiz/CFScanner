package ir.filternet.cfscanner.model

import ir.filternet.cfscanner.BuildConfig

data class ScanSettings(
    val worker: Float = 4.99f,
    val speedTestSize: Float = 300f,
    val fronting: String = BuildConfig.FrontingAddress,
    val autoFetch: Boolean = true,
    val shuffle: Boolean = false,
    val customRange: Boolean = false,
    val pingFilter: Float = 2900f,
    val autoSkipPortion:Float = 0f,
)
