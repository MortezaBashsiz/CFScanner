package ir.filternet.cfscanner.ui.page.sub.scan_details

import androidx.annotation.StringRes
import ir.filternet.cfscanner.contracts.ViewEvent
import ir.filternet.cfscanner.contracts.ViewSideEffect
import ir.filternet.cfscanner.contracts.ViewState
import ir.filternet.cfscanner.model.Config
import ir.filternet.cfscanner.model.Connection
import ir.filternet.cfscanner.model.Scan
import ir.filternet.cfscanner.service.CloudSpeedService
import ir.filternet.cfscanner.ui.page.sub.scan_details.component.ConnectionSort

class ScanDetailsContract {

    sealed class Event : ViewEvent {
        object StartSortAllBySpeed : Event()
        object StartSortAllByPing : Event()
        object StopSortAll : Event()
        data class ChangeSortMode(val sort: ConnectionSort) : Event()
        data class UpdateSpeed(val connection: Connection) : Event()
        object ResumeScan : Event()
        object StopScan : Event()
        object DeleteScan : Event()

    }

    data class State(
        val loading: Boolean = false,
        val scan: Scan? = null,
        val connections: List<Connection> = emptyList(),
        val configs: List<Config> = emptyList(),
        val sort: ConnectionSort = ConnectionSort.DATE,
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