package ir.filternet.cfscanner.ui.page.main

import androidx.annotation.StringRes
import ir.filternet.cfscanner.contracts.ViewEvent
import ir.filternet.cfscanner.contracts.ViewSideEffect
import ir.filternet.cfscanner.contracts.ViewState
import ir.filternet.cfscanner.model.*

class MainContract {

    sealed class Event : ViewEvent {
        data class SelectTabIndex(val index:Int) : Event()
        data class ImportConfig(val config:String) : Event()
        data class SelectConfig(val config:Config) : Event()
        data class SelectISP(val isp:ISP) : Event()
        object StartScan : Event()
        object StopScan : Event()
    }

    data class State(
        val loading: Boolean = false,
        val scanning:Boolean = false,
        val configs:List<Config> = listOf(),
        val isps:List<ISP> = listOf(),
        val connections:Map<CIDR, List<Connection>>? = null,
        val buttonState: ScanButtonState = ScanButtonState.Disabled(),
        val selectedIndex:Int = 1
    ) : ViewState

    sealed class Effect : ViewSideEffect {
        sealed class Messenger : Effect() {
            data class Toast(val message:String? = null , @StringRes val messageId:Int? = null) : Messenger()
        }

        sealed class Navigation : Effect() {
            data class ToScan(val scan: Scan): Navigation()
        }
    }
}