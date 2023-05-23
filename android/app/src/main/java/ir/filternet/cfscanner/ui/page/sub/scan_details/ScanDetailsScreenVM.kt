package ir.filternet.cfscanner.ui.page.sub.scan_details

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.filternet.cfscanner.contracts.BaseViewModel
import ir.filternet.cfscanner.model.Connection
import ir.filternet.cfscanner.repository.ConfigRepository
import ir.filternet.cfscanner.repository.ConnectionRepository
import ir.filternet.cfscanner.repository.ScanRepository
import ir.filternet.cfscanner.service.CloudScannerService
import ir.filternet.cfscanner.service.CloudSpeedService
import ir.filternet.cfscanner.ui.page.sub.scan_details.component.ConnectionSort
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
class ScanDetailsScreenVM @Inject constructor(
    private val connectionRepository: ConnectionRepository,
    private val scanRepository: ScanRepository,
    private val configRepository: ConfigRepository,
) : BaseViewModel<ScanDetailsContract.Event, ScanDetailsContract.State, ScanDetailsContract.Effect>(),
    ServiceConnection,
    CloudSpeedService.CloudSpeedService,
    CloudScannerService.CloudScannerServiceListener {

    private var scannerBinder: CloudScannerService.CloudScannerServiceBinder? = null
    private var speedBinder: CloudSpeedService.CloudSpeedServiceBinder? = null

    private var scanID = -1
    private var sorting:ConnectionSort = ConnectionSort.DATE

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

            ScanDetailsContract.Event.StartSortAllByPing -> {
                speedBinder?.startCheck(viewState.value.connections, true)
            }

            ScanDetailsContract.Event.StopSortAll -> {
                speedBinder?.stopCheck()
            }

            ScanDetailsContract.Event.StopScan -> {
                scannerBinder?.pauseScan()
            }

            is ScanDetailsContract.Event.ChangeSortMode -> {
                changeSorting(event.sort)
            }
        }
    }

    private fun changeSorting(sort: ConnectionSort) {
        this.sorting = sort
        setState { copy(loading=true , sort = sort) }
        setConnectionBySort(viewState.value.connections)
        setState { copy(loading=false) }
    }

    private fun deleteScanHistory() = vmScope {
        setState {
            copy(loading = true, scan = null, connections = emptyList())
        }
        scanRepository.getScanById(scanID)?.let {
            connectionRepository.deleteByScanID(it)
            scanRepository.deleteScan(it)
        }
        scannerBinder?.removeServiceListener()
        speedBinder?.removeListener()
        speedBinder?.stopCheck()
        setState {
            copy(loading = false, scan = null, connections = emptyList())
        }
        setEffect { ScanDetailsContract.Effect.Navigation.ToUp }
    }

    override fun onCleared() {
        scannerBinder?.removeServiceListener()
        speedBinder?.removeListener()
        Timber.d("DetailsScanScreenVM onCleared")
        super.onCleared()
    }

    private fun requestScanDetails() = vmScope {
        setState { copy(loading = true) }
        val scan = scanRepository.getScanById(scanID)

        if (scan == null) {
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

        val connections = connectionRepository.getAllConnectionByScanID(scan).distinctBy { it.ip }
        Timber.e("DetailsScanScreenVM ==> Scan connection count: ${connections.size}")
        setState { copy(loading = false) }
        setConnectionBySort(connections)

        // fetch last status from service
        speedBinder?.getLastStatus()?.let {
            onStatusChange(it)
        }

    }

    // need to share for different configs
    private fun requestConfigs() = vmScope {
        val configs = configRepository.getAllConfig()
        setState { copy(configs = configs) }
    }

    private fun setConnectionBySort(list: List<Connection>) {
       val list = list.let {
            when(sorting){
                ConnectionSort.DATE -> {
                    it.sortedByDescending { it.update }
                }
                ConnectionSort.PING -> {
                    it.sortedBy { it.delay }
                }
                ConnectionSort.SPEED -> {
                    it.sortedByDescending { it.speed }
                }
            }
        }
        setState { copy(connections =list) }
    }

    fun setScan(scanID: Int) {
        this.scanID = scanID
        requestScanDetails()
        requestConfigs()
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        when {
            name?.shortClassName?.contains("CloudScannerService") == true -> {
                scannerBinder = service as CloudScannerService.CloudScannerServiceBinder
                scannerBinder?.setServiceListener(this)
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
        Timber.d("CidrManagementScreenVM:  ${status}")
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
        if (status is CloudSpeedService.SpeedServiceStatus.Idle) {
            val connections = viewState.value.connections.map { it.copy(updating = false) }.toMutableList()
            setConnectionBySort(connections)
        }


    }

    override fun onServiceStatusChanged(status: CloudScannerService.ServiceStatus?) {

        val scanning = status is CloudScannerService.ServiceStatus.Scanning
        val deletable = status?.let { !(it is CloudScannerService.ServiceStatus.Scanning && it.scan.uid == this.scanID) } ?: true

        setState {
            copy(
                scanning = scanning,
                deleteable = deletable
            )
        }
    }


}