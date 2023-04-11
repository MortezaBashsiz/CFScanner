package ir.filternet.cfscanner.ui.page.main

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.filternet.cfscanner.R
import ir.filternet.cfscanner.contracts.BaseViewModel
import ir.filternet.cfscanner.model.Config
import ir.filternet.cfscanner.model.Connection
import ir.filternet.cfscanner.model.ISP
import ir.filternet.cfscanner.model.ScanButtonState
import ir.filternet.cfscanner.offline.TinyStorage
import ir.filternet.cfscanner.repository.ConfigRepository
import ir.filternet.cfscanner.repository.ConnectionRepository
import ir.filternet.cfscanner.repository.ISPRepository
import ir.filternet.cfscanner.scanner.v2ray.V2rayConfigUtil
import ir.filternet.cfscanner.service.CloudScannerService
import ir.filternet.cfscanner.service.NetworkManager
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
class MainScreenVM @Inject constructor(
    private val configRepository: ConfigRepository,
    private val connectionRepository: ConnectionRepository,
    private val ispRepository: ISPRepository,
    private val tinyStorage: TinyStorage,
) : BaseViewModel<MainContract.Event, MainContract.State, MainContract.Effect>() {

    init {
        Timber.d("MainScreenVM init")
    }

    override fun setInitialState(): MainContract.State {
        return MainContract.State()
    }

    override fun handleEvents(event: MainContract.Event) {
        when (event) {
            is MainContract.Event.SelectTabIndex -> {
                setState { copy(selectedIndex = event.index) }
            }
            else -> {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("MainScreenVM onCleared")
    }

}