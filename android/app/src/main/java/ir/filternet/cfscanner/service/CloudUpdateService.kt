package ir.filternet.cfscanner.service

import android.app.Service
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.coolerfall.download.DownloadCallback
import com.coolerfall.download.DownloadManager
import com.coolerfall.download.DownloadRequest
import com.coolerfall.download.DownloadState
import dagger.hilt.android.AndroidEntryPoint
import ir.filternet.cfscanner.model.Update
import ir.filternet.cfscanner.model.UpdateState
import ir.filternet.cfscanner.offline.TinyStorage
import java.io.File
import javax.inject.Inject


@AndroidEntryPoint
class CloudUpdateService :
    Service(),
    DownloadCallback {

    @Inject
    lateinit var tinyStorage: TinyStorage

    private val notificationManager: UpdateNotificationManager by lazy { UpdateNotificationManager(this) }

    private val binder = CloudUpdateServiceBinder()

    private val NOTIFICATION_ID = 94537

    private val downloadManager: DownloadManager by lazy {
        DownloadManager.Builder()
            .context(this)
            .build()
    }

    private var listener: CloudUpdateServiceListener? = null

    private var lastStatus: UpdateState = UpdateState.Idle

    private var update: Update? = null

    companion object {
        val COMMAND_STOP = "COMMAND_STOP"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            CloudSpeedService.COMMAND_STOP -> {
                stopForeground(true)
                downloadManager.cancelAll()
                stopSelf()
            }

            else -> {}
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        notificationManager.clearNotification(NOTIFICATION_ID)
        stopForeground(true)
        super.onDestroy()
    }

    private fun checkDownloadExistence(update: Update) {
        this.update = update
        val link = update.downloadLink

        if (update.getFile() != null) {
            if (update.isDownloaded()) {
                setUpdateState(UpdateState.Downloaded(update.getFile()!!))
            } else if (downloadManager.isDownloading(link)) {
                setUpdateState(UpdateState.Downloading(update.getProgressPercent()))
            } else {
                update.getFile()!!.delete()
            }
            return
        }


        when (downloadManager.query(link)) {
            DownloadState.PENDING, DownloadState.RUNNING -> {
                setUpdateState(UpdateState.Downloading(update.getProgressPercent()))
            }

            DownloadState.SUCCESSFUL -> {
                setUpdateState(UpdateState.Downloaded(update.getFile()!!))
            }

            else -> {
                setUpdateState(UpdateState.Idle)
            }
        }
    }

    private fun startDownloadUpdate() {
        val link = update?.downloadLink ?: return
        val request: DownloadRequest = DownloadRequest.Builder()
            .url(link)
            .downloadCallback(this)
            .build()
        downloadManager.add(request)
        checkDownloadExistence(update!!)
    }

    private fun cancelDownloadUpdate() {
        downloadManager.cancelAll()
        setUpdateState(UpdateState.Idle)
    }


    private fun setUpdateState(state: UpdateState) {
        lastStatus = state
        update = update?.copy(state = state)
        update?.let {
            listener?.onUpdateStatusChange(it)
        }

    }


    private fun Update.downloadPath(): String {
        val fileName = downloadFileName()
        return dataDir.path + "/" + fileName
    }

    private fun Update.downloadFileName(): String {
        val url = downloadLink
        return (url.split("/").lastOrNull() ?: "update.apk")
    }

    private fun Update.getFile(): File? {
        val path = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val fileName = downloadFileName()
        val file = File(path, fileName)
        if (file.exists()) {
            return file
        }
        return null
    }

    private fun Update.getProgressPercent(): Float {
        val size = (size * 1f)
        val downloaded = getFile()?.length() ?: 0L
        return downloaded / (size * 1f)
    }

    private fun Update.isDownloaded(): Boolean {
        return getProgressPercent() == 1f
    }


    override fun onFailure(downloadId: Int, statusCode: Int, errMsg: String?) {
        setUpdateState(UpdateState.Idle)
        notificationManager.clearNotification(NOTIFICATION_ID)
    }

    override fun onProgress(downloadId: Int, bytesWritten: Long, totalBytes: Long) {
        val progress = (bytesWritten * 1f) / (totalBytes * 1f)
        setUpdateState(UpdateState.Downloading(progress))
        notificationManager.notify(NOTIFICATION_ID, notificationManager.getDownloadingNotification(progress*100f))
    }

    override fun onStart(downloadId: Int, totalBytes: Long) {
        setUpdateState(UpdateState.Downloading(0.01f))
        startForeground(NOTIFICATION_ID, notificationManager.getStartDownloadNotification())
    }

    override fun onSuccess(downloadId: Int, filepath: String) {
        setUpdateState(UpdateState.Downloaded(File(filepath)))
        notificationManager.notify(NOTIFICATION_ID, notificationManager.getFinishedDownloadNotification())
    }

    inner class CloudUpdateServiceBinder : Binder() {

        fun checkUpdate(update: Update) {
            checkDownloadExistence(update)
        }

        fun startDownloadUpdate() {
            this@CloudUpdateService.startDownloadUpdate()
        }

        fun stopDownloadUpdate() {
            this@CloudUpdateService.cancelDownloadUpdate()
        }

        fun setUpdateListener(listener: CloudUpdateServiceListener) {
            this@CloudUpdateService.listener = listener
            setUpdateState(lastStatus)
        }

        fun removeListener() {
            this@CloudUpdateService.listener = null
        }
    }


    interface CloudUpdateServiceListener {
        fun onUpdateStatusChange(status: Update)
    }

}