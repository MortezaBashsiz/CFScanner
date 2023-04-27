package ir.filternet.cfscanner.ui.page.sub.cidr_management

import androidx.annotation.WorkerThread
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.filternet.cfscanner.R
import ir.filternet.cfscanner.contracts.BaseViewModel
import ir.filternet.cfscanner.model.CIDR
import ir.filternet.cfscanner.offline.TinyStorage
import ir.filternet.cfscanner.repository.CIDRRepository
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
class CidrManagementScreenVM @Inject constructor(
    private val cidrRepository: CIDRRepository,
    private val tinyStorage: TinyStorage,
) : BaseViewModel<CidrManagementContract.Event, CidrManagementContract.State, CidrManagementContract.Effect>() {


    init {
        Timber.d("CidrManagementScreenVM init")

        vmScope {
            tinyStorage.scanSettings.apply {
                setState {
                    copy(
                        autofetch = this@apply.autoFetch,
                        shuffle = this@apply.shuffle,
                        customRange = this@apply.customRange
                    )
                }
                getCidrList()
            }
        }

    }


    override fun setInitialState(): CidrManagementContract.State {
        return CidrManagementContract.State(loading = true)
    }

    override fun handleEvents(event: CidrManagementContract.Event) {
        when (event) {
            is CidrManagementContract.Event.AddIpRanges -> {
                addCidr(event.ranges)
            }

            is CidrManagementContract.Event.RemoveCIDR -> {
                removeCidr(event.cidr)
            }

            is CidrManagementContract.Event.MoveCidr -> {
                saveCidrMoves(event.from, event.to)
            }

            CidrManagementContract.Event.SaveCidrs -> {
                saveCIdrAfterMovement()
            }

            is CidrManagementContract.Event.AutoFetchChange -> {
                saveAutoFetchState(event.enabled)
            }

            is CidrManagementContract.Event.ShuffleChange -> {
                saveShuffleState(event.enabled)
            }

            is CidrManagementContract.Event.CustomRange -> {
                saveCustomRangeState(event.enabled)
            }
        }
    }

    @WorkerThread
    private fun getCidrList() = vmScope(Dispatchers.IO) {
        cidrRepository.getAllCIDR()
            .let {
                if (viewState.value.customRange)
                    it.filter { it.custom }
                else
                    it
            }
            .sortedBy { it.position }
            .let {
                setState { copy(loading = false, cidrs = it) }
            }
    }


    private fun saveShuffleState(enabled: Boolean) {
        tinyStorage.scanSettings.copy(shuffle = enabled).apply {
            tinyStorage.scanSettings = this
            setState { copy(shuffle = enabled) }
        }
    }

    private fun saveCustomRangeState(enabled: Boolean) {

        // if there is not any custom range , then prevent user from enabling custom range
        if (viewState.value.cidrs.none { it.custom }) {
            setEffect { CidrManagementContract.Effect.Messenger.Toast(messageId = R.string.there_is_not_any_custom_range) }
            return
        }


        tinyStorage.scanSettings.copy(customRange = enabled).apply {
            tinyStorage.scanSettings = this
            setState { copy(customRange = enabled) }
            getCidrList()
        }
    }

    private fun saveAutoFetchState(enabled: Boolean) {
        tinyStorage.scanSettings.copy(autoFetch = enabled).apply {
            tinyStorage.scanSettings = this
            setState { copy(autofetch = enabled) }
        }
    }

    private fun saveCidrMoves(from: Int, to: Int) {
        setState {
            val newCidrs = cidrs.toMutableList().apply {
                add(to, removeAt(from).copy(position = to))
                add(from, removeAt(from).copy(position = from))
            }
            copy(cidrs = newCidrs)
        }
    }

    private fun saveCIdrAfterMovement() = vmScope(Dispatchers.IO) {
        val pairPosition = viewState.value.cidrs
            .map { it.uid to it.position }
            .sortedBy { it.second }
            .toMap()

        val list = cidrRepository.getAllCIDR()
            .sortedBy { it.position }
            .mapIndexed { index, cidr ->
                cidr.copy(position = pairPosition.get(cidr.uid) ?: (pairPosition.size + index))
            }

        saveCidrList(list)
        getCidrList()
    }

    private suspend fun saveCidrList(list: List<CIDR>) {
        list.sortedBy { it.position }.mapIndexed { index, cidr ->
            cidr.copy(position = index)
        }.let {
            cidrRepository.updateCidrPositions(it)
        }
    }

    private fun removeCidr(cidr: CIDR) {
        vmScope(Dispatchers.IO) {

            // delete from database
            cidrRepository.deleteCIdr(cidr)


            // update positions after item in list
            val newCidr = cidrRepository.getAllCIDR()
                .toMutableList()

            // disable custom range if empty
            if (newCidr.none { it.custom }) {
                saveCustomRangeState(false)
            }

            // update ui and db
            saveCidrList(newCidr)
            getCidrList()

        }
    }

    private fun addCidr(rawCidrs: List<String>) {
        vmScope(Dispatchers.IO) {

            val allItems = cidrRepository.getAllCIDR()
                .sortedBy { it.position }.toMutableList()

            val newCidr = rawCidrs.map {
                val (address, mask) = it.split("/")
                CIDR(address, mask.toInt(), custom = true)
            }


            val shouldRemove = allItems
                .filter { newCidr.map { it.address + "/" + it.subnetMask }.contains(it.address + "/" + it.subnetMask) }
                .onEach {
                    cidrRepository.deleteCIdr(it)
                }

            allItems.removeAll(shouldRemove)

            // update positions after item in list
            val combinedCidr = allItems
                .apply { addAll(0, newCidr) }

            saveCidrList(combinedCidr)
            getCidrList()
        }
    }


}