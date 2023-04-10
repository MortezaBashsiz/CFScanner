package ir.filternet.cfscanner.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_DEFAULT
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.filternet.cfscanner.R
import ir.filternet.cfscanner.contracts.NotificationManager
import ir.filternet.cfscanner.ui.MainActivity


class ScannerNotificationManager constructor(@ApplicationContext private val context: Context) : NotificationManager(context) {

    private var contentIntent: PendingIntent? = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private val SCAN_CHANNEL_ID = "SCAN_CHANNEL"
    private val scanChannel = NotificationChannelCompat.Builder(SCAN_CHANNEL_ID, IMPORTANCE_DEFAULT)
        .setName(context.getString(R.string.scanner_channel))
        .setDescription(context.getString(R.string.scanner_channel_description))
        .build()


    private val prebuiltScanNotification = NotificationCompat.Builder(context, SCAN_CHANNEL_ID)
        .setSmallIcon(R.drawable.cf_logo)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)


    private val resumeAction = getResumeAction()
    private val pauseAction = getPauseAction()
    private val closeAction = getCloseAction()

    init {
        notificationManager.createNotificationChannel(scanChannel)
    }


    fun getScanningNotification(scannedCount: Int, successCount: Int): Notification {
        val state = context.getString(R.string.ongoing)
        val content = context.getString(R.string.notif_scanning_content, scannedCount, successCount)
        return prebuiltScanNotification
            .setContentText(content)
            .setSubText(state)
            .setContentIntent(contentIntent)
            .setSound(null)
            .setOngoing(true)
            .setSilent(true)
            .clearActions()
            .addAction(pauseAction)
            .addAction(closeAction)
            .build()
    }

    fun getScanStartedNotification(): Notification {
        val state = context.getString(R.string.preparing)
        val content = context.getString(R.string.notif_starting_scan)
        return prebuiltScanNotification
            .setContentText(content)
            .setSubText(state)
            .setContentIntent(contentIntent)
            .setSound(null)
            .setOngoing(true)
            .setSilent(true)
            .clearActions()
            .addAction(pauseAction)
            .addAction(closeAction)
            .build()
    }

    fun getScanPausedNotification(scanId: Int, scannedCount: Int, successCount: Int, cause: String = ""): Notification {
        val state = context.getString(R.string.paused)
        val content = cause.let { if (!it.isEmpty()) it + "\n" else it } + context.getString(R.string.notif_scanning_content, scannedCount, successCount)
        val rawIntent = Intent(Intent.ACTION_VIEW, "cfscanner://scandetails/?id=$scanId".toUri())
        val contentIntent = PendingIntent.getActivity(context, 0, rawIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return prebuiltScanNotification
            .setContentText(content)
            .setSubText(state)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setSilent(false)
            .clearActions()
            .addAction(resumeAction)
            .addAction(closeAction)
            .build()
    }


    fun getScanFinishedNotification(scanId: Int, successCount: Int): Notification {
        val state = context.getString(R.string.finished)
        val content = context.getString(R.string.scan_finished_by, successCount)
        val rawIntent = Intent(Intent.ACTION_VIEW, "cfscanner://scandetails/?id=$scanId".toUri())
        val contentIntent = PendingIntent.getActivity(context, 0, rawIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return prebuiltScanNotification
            .setContentText(content)
            .setSubText(state)
            .setContentIntent(contentIntent)
            .setOngoing(false)
            .setSilent(false)
            .clearActions()
            .build()
    }

    private fun getResumeAction(): NotificationCompat.Action {
        val intent = Intent(context, CloudScannerService::class.java).apply { action = CloudScannerService.COMMAND_RESUME }
        val pIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Action.Builder(null, context.getString(R.string.resume), pIntent).build()
    }

    private fun getPauseAction(): NotificationCompat.Action {
        val intent = Intent(context, CloudScannerService::class.java).apply { action = CloudScannerService.COMMAND_PAUSE }
        val pIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Action.Builder(null, context.getString(R.string.pause), pIntent).build()
    }

    private fun getCloseAction(): NotificationCompat.Action {
        val intent = Intent(context, CloudScannerService::class.java).apply { action = CloudScannerService.COMMAND_TERMINATE }
        val pIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Action.Builder(null, context.getString(R.string.close), pIntent).build()
    }


}