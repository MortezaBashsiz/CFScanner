package ir.filternet.cfscanner.ui.page.root

import androidx.annotation.StringRes
import ir.filternet.cfscanner.contracts.ViewEvent
import ir.filternet.cfscanner.contracts.ViewSideEffect
import ir.filternet.cfscanner.contracts.ViewState
import ir.filternet.cfscanner.model.CIDR
import ir.filternet.cfscanner.model.Config
import ir.filternet.cfscanner.model.Connection
import ir.filternet.cfscanner.model.ISP
import ir.filternet.cfscanner.model.Scan
import ir.filternet.cfscanner.model.ScanButtonState
import ir.filternet.cfscanner.model.Update

class MainContract {

    sealed class Event : ViewEvent {
        data class SelectTabIndex(val index:Int) : Event()
        object StartDownloadUpdate : Event()
        object StopDownloadUpdate : Event()
    }

    data class State(
        val loading: Boolean = false,
        val scanning:Boolean = false,
        val configs:List<Config> = listOf(),
        val isps:List<ISP> = listOf(),
        val connections:Map<CIDR, List<Connection>>? = null,
        val buttonState: ScanButtonState = ScanButtonState.Disabled(),
        val selectedIndex:Int = 1,
        val update:Update? = null,
    ) : ViewState

    sealed class Effect : ViewSideEffect {
        sealed class Messenger : Effect() {
            data class Toast(val message:String? = null , @StringRes val messageId:Int? = null) : Messenger()
        }

        sealed class Navigation : Effect() {
            data class ToScan(val scan: Scan): Navigation()
            object ToCidrManagement: Navigation()
        }


        data class UpdateAvailable(val update:Update) : Effect()


    }
}