package ir.filternet.cfscanner.ui.page.root

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.filternet.cfscanner.contracts.BaseViewModel
import ir.filternet.cfscanner.model.Update
import ir.filternet.cfscanner.model.UpdateState
import ir.filternet.cfscanner.offline.TinyStorage
import ir.filternet.cfscanner.repository.ConfigRepository
import ir.filternet.cfscanner.repository.ConnectionRepository
import ir.filternet.cfscanner.repository.ISPRepository
import ir.filternet.cfscanner.repository.UpdateRepository
import ir.filternet.cfscanner.service.CloudScannerService
import ir.filternet.cfscanner.service.CloudUpdateService
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
class MainScreenVM @Inject constructor(
    private val updateRepository: UpdateRepository,
    private val configRepository: ConfigRepository,
    private val connectionRepository: ConnectionRepository,
    private val ispRepository: ISPRepository,
    private val tinyStorage: TinyStorage,
) : BaseViewModel<MainContract.Event, MainContract.State, MainContract.Effect>(),
    ServiceConnection,
    CloudUpdateService.CloudUpdateServiceListener {

    private var binder: CloudUpdateService.CloudUpdateServiceBinder? = null

    init {
        Timber.d("MainScreenVM init")

        vmScope(Dispatchers.IO) {
            updateRepository.checkUpdate()?.let {
                binder?.checkUpdate(update = it)
            }
        }
    }

    override fun setInitialState(): MainContract.State {
        return MainContract.State()
    }

    override fun handleEvents(event: MainContract.Event) {
        when (event) {
            is MainContract.Event.SelectTabIndex -> {
                setState { copy(selectedIndex = event.index) }
            }
            is MainContract.Event.StartDownloadUpdate ->{
                binder?.startDownloadUpdate()
            }
            is MainContract.Event.StopDownloadUpdate ->{
                binder?.stopDownloadUpdate()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("MainScreenVM onCleared")
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        binder = service as CloudUpdateService.CloudUpdateServiceBinder
        binder?.setUpdateListener(this)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        binder?.removeListener()
    }

    override fun onUpdateStatusChange(status: Update) {
        setEffect { MainContract.Effect.UpdateAvailable(status) }
    }

}