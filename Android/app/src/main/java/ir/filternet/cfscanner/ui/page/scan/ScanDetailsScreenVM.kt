package ir.filternet.cfscanner.ui.page.scan

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.filternet.cfscanner.contracts.BaseViewModel
import ir.filternet.cfscanner.model.Connection
import ir.filternet.cfscanner.repository.ConnectionRepository
import ir.filternet.cfscanner.repository.ScanRepository
import ir.filternet.cfscanner.service.CloudScannerService
import ir.filternet.cfscanner.service.CloudSpeedService
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
class ScanDetailsScreenVM @Inject constructor(
    private val connectionRepository: ConnectionRepository,
    private val scanRepository: ScanRepository,
) : BaseViewModel<ScanDetailsContract.Event, ScanDetailsContract.State, ScanDetailsContract.Effect>(), ServiceConnection, CloudSpeedService.CloudSpeedService {

    private var scannerBinder: CloudScannerService.CloudScannerServiceBinder? = null
    private var speedBinder: CloudSpeedService.CloudSpeedServiceBinder? = null

    private var scanID = -1

    init {
        Timber.d("DetailsScanScreenVM init")
    }

    override fun setInitialState(): ScanDetailsContract.State {
        return ScanDetailsContract.State()
    }

    override fun handleEvents(event: ScanDetailsContract.Event) {
        when (event) {
            is ScanDetailsContract.Event.ResumeScan -> {
                viewState.value.scan?.let {
                    scannerBinder?.startScan(it)
                }
            }
            is ScanDetailsContract.Event.UpdateSpeed -> {
                speedBinder?.startCheck(listOf(event.connection))
            }
            ScanDetailsContract.Event.DeleteScan -> {
                deleteScanHistory()
            }
            ScanDetailsContract.Event.StartSortAllBySpeed -> {
                speedBinder?.startCheck(viewState.value.connections)
            }
            ScanDetailsContract.Event.StopSortAllBySpeed -> {
                speedBinder?.stopCheck()
            }
        }
    }

    private fun deleteScanHistory() = vmScope {
        setState {
            copy(loading = true, scan = null, connections = emptyList())
        }
        scanRepository.getScanById(scanID)?.let {
            connectionRepository.deleteByScanID(it)
            scanRepository.deleteScan(it)
        }
        speedBinder?.removeListener()
        speedBinder?.stopCheck()
        setState {
            copy(loading = false, scan = null, connections = emptyList())
        }
        setEffect { ScanDetailsContract.Effect.Navigation.ToUp }
    }

    override fun onCleared() {
        speedBinder?.removeListener()
        Timber.d("DetailsScanScreenVM onCleared")
        super.onCleared()
    }

    private fun requestScanDetails() = vmScope {
        setState { copy(loading = true) }
        val scan = scanRepository.getScanById(scanID)

        if(scan==null){
            setEffect {
                ScanDetailsContract.Effect.Messenger.Toast("Scan Not Found !")
            }
            setEffect {
                ScanDetailsContract.Effect.Navigation.ToUp
            }
            return@vmScope
        }
        Timber.e("DetailsScanScreenVM ==> Scan get: $scan")
        setState { copy(loading = true, scan = scan) }

        val connections = connectionRepository.getAllConnectionByScanID(scan)
        Timber.e("DetailsScanScreenVM ==> Scan connection count: ${connections.size}")
        setState { copy(loading = false) }
        setConnectionBySort(connections)

        // fetch last status from service
        speedBinder?.getLastStatus()?.let {
            onStatusChange(it)
        }

    }

    private fun setConnectionBySort(list: List<Connection>) {
        setState { copy(connections = list.sortedByDescending { it.speed }) }
    }

    fun setScan(scanID: Int) {
        this.scanID = scanID
        requestScanDetails()
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        when {
            name?.shortClassName?.contains("CloudScannerService") == true -> {

                scannerBinder = service as CloudScannerService.CloudScannerServiceBinder
                val scanning = scannerBinder?.getServiceStatus() is CloudScannerService.ServiceStatus.Scanning
                val deletable = scannerBinder?.getServiceStatus()?.let { !(it is CloudScannerService.ServiceStatus.Scanning && it.scan.uid == this.scanID) } ?: true

                setState {
                    copy(
                        scanning = scanning,
                        deleteable = deletable
                    )
                }
            }
            name?.shortClassName?.contains("CloudSpeedService") == true -> {
                speedBinder = service as CloudSpeedService.CloudSpeedServiceBinder
                speedBinder?.setListener(this)
            }
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        when {
            name?.shortClassName?.contains("CloudScannerService") == true -> {
                scannerBinder = null
            }
            name?.shortClassName?.contains("CloudSpeedService") == true -> {
                speedBinder = null
            }
        }

    }

    override fun onStatusChange(status: CloudSpeedService.SpeedServiceStatus) {
        Timber.d("ScanDetailsScreenVM:  ${status}")
        if (status is CloudSpeedService.SpeedServiceStatus.Checking) {
            val connections = viewState.value.connections.toMutableList()
            val newConnection = status.connection
            val connectionIndex = connections.indexOfFirst { it.id == newConnection.id }
            if (connectionIndex >= 0) {
                connections.removeAt(connectionIndex)
                connections.add(connectionIndex, newConnection)
            }
            setConnectionBySort(connections)
        }

        if (status is CloudSpeedService.SpeedServiceStatus.Checking && status.connection.scan?.uid != scanID)
            setState { copy(speedStatus = CloudSpeedService.SpeedServiceStatus.Disable) }
        else
            setState { copy(speedStatus = status) }

        // make sure all item updating status is Idle
        if(status is CloudSpeedService.SpeedServiceStatus.Idle){
            val connections = viewState.value.connections.map { it.copy(updating = false) }.toMutableList()
            setConnectionBySort(connections)
        }


    }


}