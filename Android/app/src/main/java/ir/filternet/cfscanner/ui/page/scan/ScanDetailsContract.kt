package ir.filternet.cfscanner.ui.page.scan

import androidx.annotation.StringRes
import ir.filternet.cfscanner.contracts.ViewEvent
import ir.filternet.cfscanner.contracts.ViewSideEffect
import ir.filternet.cfscanner.contracts.ViewState
import ir.filternet.cfscanner.model.Connection
import ir.filternet.cfscanner.model.Scan
import ir.filternet.cfscanner.service.CloudSpeedService

class ScanDetailsContract {

    sealed class Event : ViewEvent {
        object StartSortAllBySpeed : Event()
        object StopSortAllBySpeed : Event()
        data class UpdateSpeed(val connection: Connection) : Event()
        object ResumeScan : Event()
        object DeleteScan : Event()
    }

    data class State(
        val loading: Boolean = false,
        val scan: Scan? = null,
        val connections: List<Connection> = emptyList(),
        val scanning: Boolean = true,
        val deleteable:Boolean = true,
        val speedStatus:CloudSpeedService.SpeedServiceStatus = CloudSpeedService.SpeedServiceStatus.Disable
    ) : ViewState

    sealed class Effect : ViewSideEffect {
        sealed class Messenger : Effect() {
            data class Toast(val message: String? = null, @StringRes val messageId: Int? = null) : Messenger()
        }

        sealed class Navigation : Effect() {
            object ToUp : Navigation()
        }
    }
}