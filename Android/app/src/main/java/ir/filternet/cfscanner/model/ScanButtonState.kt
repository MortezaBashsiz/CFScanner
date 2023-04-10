package ir.filternet.cfscanner.model

sealed class ScanButtonState {
    object Ready : ScanButtonState()
    data class Scanning(val scan:Scan ,val progress: ScanProgress) : ScanButtonState()
    data class Disabled(val message:String = "") : ScanButtonState()
    data class Paused(val message:String = "") : ScanButtonState()
}