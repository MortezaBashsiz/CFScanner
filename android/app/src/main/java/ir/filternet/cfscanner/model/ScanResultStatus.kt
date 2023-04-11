package ir.filternet.cfscanner.model

enum class ScanResultStatus(val value: Int) {
    PAUSED(0),
    FINISHED(1),
}


fun getScanResultStatus(value: Int?): ScanResultStatus {
    return when(value) {
        0 -> ScanResultStatus.PAUSED
        1 -> ScanResultStatus.FINISHED
        else -> ScanResultStatus.PAUSED
    }
}