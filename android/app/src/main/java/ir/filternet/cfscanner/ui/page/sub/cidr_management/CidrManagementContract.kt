package ir.filternet.cfscanner.ui.page.sub.cidr_management

import androidx.annotation.StringRes
import ir.filternet.cfscanner.contracts.ViewEvent
import ir.filternet.cfscanner.contracts.ViewSideEffect
import ir.filternet.cfscanner.contracts.ViewState
import ir.filternet.cfscanner.model.CIDR

class CidrManagementContract{

    sealed class Event : ViewEvent {
        data class AddIpRanges(val ranges: List<String>) : Event()
        data class RemoveCIDR(val cidr: CIDR) : Event()
        data class MoveCidr(val from: Int, val to: Int) : Event()
        data class ShuffleChange(val enabled:Boolean = false) : Event()
        data class CustomRange(val enabled:Boolean = false) : Event()
        data class AutoFetchChange(val enabled:Boolean = false) : Event()
        object SaveCidrs: Event()
    }

    data class State(
        val loading: Boolean = false,
        val autofetch:Boolean = true,
        val shuffle:Boolean = false,
        val customRange:Boolean = false,
        val cidrs : List<CIDR> = listOf(),
    ) : ViewState

    sealed class Effect : ViewSideEffect {
        sealed class Messenger : Effect() {
            data class Toast(val message: String? = null, @StringRes val messageId: Int? = null) : Messenger()
        }

        sealed class Navigation : Effect() {
            object NavigateUP : Navigation()
        }
    }
}