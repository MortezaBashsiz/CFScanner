package ir.filternet.cfscanner.ui.page.sub.scan

import androidx.annotation.StringRes
import ir.filternet.cfscanner.contracts.ViewEvent
import ir.filternet.cfscanner.contracts.ViewSideEffect
import ir.filternet.cfscanner.contracts.ViewState
import ir.filternet.cfscanner.model.*

class ScanContract {

    sealed class Event : ViewEvent {
        object StartScan : Event()
        object StopScan : Event()
        data class DeleteConfig(val config: Config) : Event()
        data class UpdateConfig(val config: Config) : Event()
        data class AddConfig(val config: String) : Event()
        data class SelectConfig(val config: Config) : Event()
    }

    data class State(
        val loading: Boolean = false,
        val configs: List<Config> = emptyList(),
        val scan: Scan? = null,
        val logs: List<Log> = emptyList(),
        val buttonState: ScanButtonState = ScanButtonState.Disabled(),
        val configEdit: Config? = null,
    ) : ViewState

    sealed class Effect : ViewSideEffect {

        sealed class Messenger : Effect() {
            data class Toast(val message: String? = null, @StringRes val messageId: Int? = null) : Messenger()
        }

        sealed class Navigation : Effect() {}

        sealed class Dialog : Effect() {
            data class ConfigEditDialog(val config: Config) : Dialog()
        }
    }
}