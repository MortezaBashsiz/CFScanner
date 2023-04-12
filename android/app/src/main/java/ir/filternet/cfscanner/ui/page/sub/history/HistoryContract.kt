package ir.filternet.cfscanner.ui.page.sub.history

import androidx.annotation.StringRes
import ir.filternet.cfscanner.contracts.ViewEvent
import ir.filternet.cfscanner.contracts.ViewSideEffect
import ir.filternet.cfscanner.contracts.ViewState
import ir.filternet.cfscanner.model.Scan

class HistoryContract {

    sealed class Event : ViewEvent

    data class State(
        val loading:Boolean = false,
        val scans: List<Scan> = emptyList(),
    ) : ViewState

    sealed class Effect : ViewSideEffect {
        sealed class Messenger : Effect() {
            data class Toast(val message: String? = null, @StringRes val messageId: Int? = null) : Messenger()
        }

        sealed class Navigation : Effect() {
            data class ToScan(val scan:Scan) : Navigation()
        }
    }
}