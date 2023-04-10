package ir.filternet.cfscanner.ui.page.sub.history

import dagger.hilt.android.lifecycle.HiltViewModel
import ir.filternet.cfscanner.contracts.BaseViewModel
import ir.filternet.cfscanner.offline.TinyStorage
import ir.filternet.cfscanner.repository.ConfigRepository
import ir.filternet.cfscanner.repository.ConnectionRepository
import ir.filternet.cfscanner.repository.ISPRepository
import ir.filternet.cfscanner.repository.ScanRepository
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
class HistoryScreenVM @Inject constructor(
    private val configRepository: ConfigRepository,
    private val connectionRepository: ConnectionRepository,
    private val scanRepository: ScanRepository,
    private val tinyStorage: TinyStorage,
) : BaseViewModel<HistoryContract.Event, HistoryContract.State, HistoryContract.Effect>() {

    init {
        vmScope {
            val scans = scanRepository.getAllScans().reversed()
            setState {
                copy(loading = false , scans = scans)
            }
        }
    }

    override fun setInitialState(): HistoryContract.State {
        return HistoryContract.State(loading = true)
    }

    override fun handleEvents(event: HistoryContract.Event) {

    }

    override fun onCleared() {
        Timber.d("HistoryScreenVM onCleared")
        super.onCleared()
    }


}