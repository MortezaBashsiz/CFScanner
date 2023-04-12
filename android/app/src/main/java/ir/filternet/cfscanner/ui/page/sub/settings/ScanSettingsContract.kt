package ir.filternet.cfscanner.ui.page.sub.settings

import androidx.annotation.StringRes
import ir.filternet.cfscanner.contracts.ViewEvent
import ir.filternet.cfscanner.contracts.ViewSideEffect
import ir.filternet.cfscanner.contracts.ViewState
import ir.filternet.cfscanner.model.ScanSettings

class ScanSettingsContract {

    sealed class Event : ViewEvent {
        data class UpdateSettings(val settings: ScanSettings) : Event()
    }

    data class State(
        val settings: ScanSettings = ScanSettings(),
    ) : ViewState

    sealed class Effect : ViewSideEffect {
        sealed class Messenger : Effect() {
            data class Toast(val message: String? = null, @StringRes val messageId: Int? = null) : Messenger()
        }

        sealed class Navigation : Effect() {}
    }
}