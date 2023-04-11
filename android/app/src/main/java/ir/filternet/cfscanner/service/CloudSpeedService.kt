package ir.filternet.cfscanner.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import ir.filternet.cfscanner.model.Connection
import ir.filternet.cfscanner.model.Scan
import ir.filternet.cfscanner.repository.ConnectionRepository
import ir.filternet.cfscanner.scanner.CFSpeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class CloudSpeedService : Service(), CFSpeed.CFSpeedListener {

    private val NOTIFICATION_ID = 2651

    private var listener: CloudSpeedService? = null

    private var scan: Scan? = null

    private val binder = CloudSpeedServiceBinder()

    private var lastStatus: SpeedServiceStatus = SpeedServiceStatus.Idle

    private val notifManager: SpeedNotificationManager by lazy { SpeedNotificationManager(this) }

    private val scope = CoroutineScope(Dispatchers.IO)

    @Inject
    lateinit var cfspeed: CFSpeed

    @Inject
    lateinit var connectionRepository: ConnectionRepository

    companion object {
        val COMMAND_STOP = "COMMAND_STOP"
    }

    override fun onCreate() {
        super.onCreate()
        cfspeed.setListener(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            COMMAND_STOP -> {
                stopCheck()
                stopForeground(true)
                stopSelf()
            }
            else -> {}
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        cfspeed.removeListener()
        cfspeed.stopCheck()
        notifManager.clearNotification(NOTIFICATION_ID)
        listener = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        listener = null
        return super.onUnbind(intent)
    }

    inner class CloudSpeedServiceBinder : Binder() {

        fun startCheck(connection: List<Connection>) {
            this@CloudSpeedService.startCheck(connection)
        }

        fun stopCheck() {
            this@CloudSpeedService.stopCheck()
        }

        fun getLastStatus():SpeedServiceStatus{
            return lastStatus
        }


        fun setListener(listener: CloudSpeedService) {
            this@CloudSpeedService.listener = listener
            this@CloudSpeedService.listener?.onStatusChange(lastStatus)
        }

        fun removeListener() {
            this@CloudSpeedService.listener = null
        }

    }


    private fun startCheck(connections: List<Connection>) {
        cfspeed.startCheck(connections)
    }

    private fun stopCheck() {
        cfspeed.stopCheck()
    }

    private fun setState(status: SpeedServiceStatus) {
        this.lastStatus = status
        listener?.onStatusChange(status)
    }

    private fun updateConnection(connection: Connection) {
        scope.launch {
            connectionRepository.update(connection)
        }
    }

    interface CloudSpeedService {
        fun onStatusChange(status: SpeedServiceStatus)
    }


    sealed class SpeedServiceStatus {
        object Idle : SpeedServiceStatus()
        data class Checking(val connection: Connection, val progress: Float = 0f) : SpeedServiceStatus()
        object Disable : SpeedServiceStatus()
    }

    override fun onStartChecking(count: Int) {
        startForeground(NOTIFICATION_ID, notifManager.getSpeedCheckingNotification(count, 0L, scan?.uid ?: 0))
    }

    override fun onCheckProcess(connection: Connection, progress: Float, remainItem: Int, estimatedTime: Long) {
        updateConnection(connection = connection)
        setState(SpeedServiceStatus.Checking(connection, progress))

        this.scan = connection.scan

        with(notifManager) {
            notify(NOTIFICATION_ID, getSpeedCheckingNotification(remainItem, estimatedTime, scan?.uid ?: 0))
        }
    }

    override fun onFinishChecking(count: Int) {
        with(notifManager) {
            notify(NOTIFICATION_ID + Random.nextInt(), getSpeedCheckFinishedNotification(scan?.uid ?: 0))
        }
    }

    override fun onStopChecking() {
        setState(SpeedServiceStatus.Idle)
        stopForeground(true)
        notifManager.clearNotification(NOTIFICATION_ID)
    }

}