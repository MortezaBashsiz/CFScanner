package ir.filternet.cfscanner.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import ir.filternet.cfscanner.R
import ir.filternet.cfscanner.model.*
import ir.filternet.cfscanner.offline.TinyStorage
import ir.filternet.cfscanner.repository.ConnectionRepository
import ir.filternet.cfscanner.repository.ISPRepository
import ir.filternet.cfscanner.repository.ScanRepository
import ir.filternet.cfscanner.scanner.CFScanner
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class CloudScannerService : Service(),
    NetworkManager.NetworkManagerListener,
    CFScanner.CFScannerListener,
    CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()) {


    @Inject
    lateinit var tinyStorage: TinyStorage

    @Inject
    lateinit var ispRepository: ISPRepository

    @Inject
    lateinit var connectionRepository: ConnectionRepository

    @Inject
    lateinit var scanRepository: ScanRepository


    companion object {
        const val COMMAND_RESUME = "COMMAND_RESUME"
        const val COMMAND_PAUSE = "COMMAND_PAUSE"
        const val COMMAND_TERMINATE = "COMMAND_TERMINATE"
    }

    private val NOTIFICATION_ID = 57516

    private lateinit var notifManager: ScannerNotificationManager

    private val binder = CloudScannerServiceBinder()
    private lateinit var networkManager: NetworkManager

    @Inject
    lateinit var cfScanner: CFScanner
    private var listener: CloudScannerServiceListener? = null

    private var isp: ISP? = null
    private var scan: Scan? = null
    private var serviceStatus: ServiceStatus = ServiceStatus.Idle()


    /* connection flow */
    private val _connectionFlow = BehaviorSubject.create<Connection>()
    private var connectionFlow: Disposable? = null


    /* scan flow */
    private val _progressFlow = BehaviorSubject.create<Scan>()
    private var progressFlow: Disposable? = null


    override fun onCreate() {
        super.onCreate()
        notifManager = ScannerNotificationManager(this)

        Timber.d("CloudScannerService: onCreate")
        networkManager = NetworkManager(this)

        networkManager.setListener(this)
        cfScanner.setListener(this)

        networkManager.startMonitor()

        connectionFlow = _connectionFlow
            .observeOn(Schedulers.io())
            .subscribeOn(Schedulers.io())
            .buffer(5, TimeUnit.SECONDS)
            .subscribe {
                runBlocking {
                    saveConnections(it)
                }
            }

        progressFlow = _progressFlow
            .observeOn(Schedulers.io())
            .subscribeOn(Schedulers.io())
            .buffer(2, TimeUnit.SECONDS)
            .subscribe {
                runBlocking {
                    saveProgress(it.lastOrNull())
                }
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val command = intent?.action
        Timber.d("CloudScannerService: onStartCommand ==> $command")
        when (command) {
            COMMAND_RESUME -> {
                Timber.d("CloudScannerService: onStartCommand COMMAND_RESUME $scan")
                if (scan != null) {
                    startScanner(scan!!)
                } else {
                    stopSelf(true)
                }
            }
            COMMAND_PAUSE -> {
                Timber.d("CloudScannerService: onStartCommand COMMAND_PAUSE")
                cfScanner.stopScan(false, getString(R.string.scan_paused_by_you))
            }
            COMMAND_TERMINATE -> {
                Timber.d("CloudScannerService: onStartCommand COMMAND_TERMINATE")
                stopScan()
                stopSelf(true)
            }
            else -> {}
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        Timber.d("CloudScannerService: onBind")
        return binder
    }

    override fun onRebind(intent: Intent?) {
        Timber.d("CloudScannerService: onRebind")
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Timber.d("CloudScannerService: onUnbind")
        listener = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        Timber.d("CloudScannerService: onDestroy")
        connectionFlow?.dispose()
        progressFlow?.dispose()
        networkManager.removeListener()
        notifManager.clearNotification(NOTIFICATION_ID)
        super.onDestroy()
    }


    inner class CloudScannerServiceBinder : Binder() {

        fun startScan(config: Config) {
            startScanner(config)
        }

        fun startScan(scan: Scan) {
            startScanner(scan)
        }

        fun pauseScan() {
            stopScan()
        }

        fun getLastScan(): Scan? = scan

        fun getServiceStatus(): ServiceStatus = serviceStatus

        fun setServiceListener(listener: CloudScannerServiceListener) {
            this@CloudScannerService.listener = listener
            this@CloudScannerService.listener?.onServiceStatusChanged(serviceStatus)
        }

        fun removeServiceListener() {
            this@CloudScannerService.listener = null
        }

    }

    private fun startScanner(config: Config) {
        Timber.d("CloudScannerService startScanner")
        launch {
            isp?.let {
                val scanSettings = tinyStorage.scanSettings?:ScanSettings()
                val scan = scanRepository.createScan(config, it)
                cfScanner.startScan(
                    scan, CFScanner.ScanOptions(
                        parallel = scanSettings.worker.toInt(),
                        frontingDomain = scanSettings.fronting
                    )
                )
            }
        }
    }

    private fun startScanner(scan: Scan) {
        Timber.d("CloudScannerService startScanner")
        launch {
            val scanSettings = tinyStorage.scanSettings ?: ScanSettings()
            cfScanner.startScan(
                scan, CFScanner.ScanOptions(
                    parallel = scanSettings.worker.toInt(),
                    frontingDomain = scanSettings.fronting
                )
            )
        }
    }

    private fun stopScan() {
        Timber.d("CloudScannerService stopScan")
        cfScanner.stopScan(true)
    }

    /**
     * save success connection besides of last connection for each scan
     * @param connections List<Connection>
     */
    private suspend fun saveConnections(connections: List<Connection>) {
        if (connections.isEmpty()) return
        Timber.d("CloudScannerService: add connections to db: ${connections.size}")
        val healthyConnections = connections.filter { it.delay > 0 }
        val lastConnection = connections.lastOrNull()
        if (healthyConnections.isNotEmpty()) {
            connectionRepository.insertAll(healthyConnections)
        }

        lastConnection?.let {
            tinyStorage.addLastConnection(it)
        }
    }

    private suspend fun saveProgress(scan: Scan?) {
        if (scan == null) return
        Timber.d("CloudScannerService: save state to db: ${scan.progress}")
        scanRepository.updateScanProgress(scan)

        // because of observer delay must check last status to avoid publish wrong status
        if (serviceStatus is ServiceStatus.Scanning)
            with(notifManager) {
                val progress = scan.progress
                notify(NOTIFICATION_ID, getScanningNotification(progress.checkConnectionCount, progress.successConnectionCount))
            }
    }

    private fun setServiceStatus(status: ServiceStatus) {
        Timber.d("CloudScannerService setServiceStatus: $status")
        serviceStatus = status
        listener?.onServiceStatusChanged(status)
    }


    override fun onScanStart(scan: Scan, options: CFScanner.ScanOptions) {
        Timber.d("CloudScannerService onScanStart: $scan")
        startForeground(NOTIFICATION_ID, notifManager.getScanStartedNotification())
        setServiceStatus(ServiceStatus.Scanning(scan))
    }

    override fun onConnectionUpdate(scan: Scan, connection: Connection) {
        Timber.d("CloudScannerService onConnectionUpdate: $connection")
        _connectionFlow.onNext(connection)
        _progressFlow.onNext(scan)
        setServiceStatus(ServiceStatus.Scanning(scan))
    }

    override fun onScanFinished(scan: Scan) {
        Timber.d("CloudScannerService onScanFinished: $scan")
        setServiceStatus(ServiceStatus.Idle(isp))

        with(notifManager) {
            val progress = scan.progress.successConnectionCount
            notify(NOTIFICATION_ID + Random.nextInt(), getScanFinishedNotification(scan.uid, progress))
        }

        stopSelf(true)
    }

    override fun onScanPaused(scan: Scan, reason: String, byUser: Boolean) {
        Timber.d("CloudScannerService onScanPaused: $scan   Reason:$reason   byUser: $byUser")
        this.scan = scan

        if (networkManager.getNetworkState() == NetworkManager.NetworkState.DISCONNECTED) {
            setServiceStatus(ServiceStatus.Disabled(reason))
        } else {
            setServiceStatus(ServiceStatus.Paused(scan, ScanProgress(), reason))
        }

        if (byUser) {
            stopForeground(true)
        } else {
            with(notifManager) {
                val progress = scan.progress
                notify(NOTIFICATION_ID, getScanPausedNotification(scan.uid, progress.checkConnectionCount, progress.successConnectionCount, reason))
            }
        }
    }


    override fun onNetworkStatusUpdate(status: NetworkManager.NetworkState) {
        Timber.d("CloudScannerService onNetworkStatusUpdate: $status")
        when (status) {
            NetworkManager.NetworkState.CONNECTED -> {
                if (!cfScanner.isRunning() && scan != null) {
                    startScanner(scan!!)
                } else {
                    setServiceStatus(ServiceStatus.Idle(isp))
                }
            }
            NetworkManager.NetworkState.WAITING -> {
                if (cfScanner.isRunning()) {
                    cfScanner.stopScan(byUser = false, reason = "No internet connection")
                }
                setServiceStatus(ServiceStatus.Disabled("No internet connection"))
            }
            NetworkManager.NetworkState.DISCONNECTED -> {
                if (cfScanner.isRunning()) {
                    cfScanner.stopScan(byUser = false, reason = "No internet connection")
                }
            }
        }
    }

    override fun onNetworkIspChanged(isp: ISP?) {
        Timber.d("CloudScannerService onNetworkIspChanged: $isp")
        launch {
            isp?.let {
                this@CloudScannerService.isp = ispRepository.addISP(it)
            }
        }
    }

    private fun stopSelf(clearNotif: Boolean = true) {
        stopForeground(clearNotif)
        stopSelf()
    }

    interface CloudScannerServiceListener {
        fun onServiceStatusChanged(status: ServiceStatus?)
    }


    sealed class ServiceStatus {
        data class Idle(val isp: ISP? = null) : ServiceStatus()
        data class Scanning(val scan: Scan) : ServiceStatus()
        data class Paused(val scan: Scan, val progress: ScanProgress, val message: String) : ServiceStatus()
        data class Disabled(val message: String) : ServiceStatus()
    }
}