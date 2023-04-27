package ir.filternet.cfscanner.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.filternet.cfscanner.R
import ir.filternet.cfscanner.contracts.NotificationManager
import ir.filternet.cfscanner.ui.MainActivity
import ir.filternet.cfscanner.utils.round

class UpdateNotificationManager constructor(@ApplicationContext private val context: Context) : NotificationManager(context) {

    private var contentIntent: PendingIntent? = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private val Update_CHANNEL_ID = "UPDATE_CHANNEL"
    private val speedChannel = NotificationChannelCompat.Builder(Update_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
        .setName(context.getString(R.string.update_channel))
        .setDescription(context.getString(R.string.update_channel_description))
        .build()

    private val prebuiltSpeedNotification = NotificationCompat.Builder(context, Update_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_update_rocket)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)

    init {
        notificationManager.createNotificationChannel(speedChannel)
    }

    private val closeAction = getCloseAction()


    fun getStartDownloadNotification(): Notification {
        val state = context.getString(R.string.preparing)
        val content = context.getString(R.string.preparing_download)
        val title = context.getString(R.string.download_preparing)
        return prebuiltSpeedNotification
            .setContentTitle(title)
            .setContentText(content)
            .setSubText(state)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setSilent(true)
            .clearActions()
            .build()
    }

    fun getDownloadingNotification(percent: Float): Notification {
        val state = context.getString(R.string.ongoing)
        val content = context.getString(R.string.downloading_update_file, percent.toInt())
        val title = context.getString(R.string.downloading)
        return prebuiltSpeedNotification
            .setContentTitle(title)
            .setContentText(content)
            .setSubText(state)
            .setContentIntent(contentIntent)
            .setProgress(100, percent.toInt(), false)
            .setOngoing(true)
            .setSilent(true)
            .clearActions()
            .addAction(closeAction)
            .build()
    }


    fun getFinishedDownloadNotification(): Notification {
        val state = context.getString(R.string.finished)
        val title = context.getString(R.string.download_finished)
        val content = context.getString(R.string.download_update_finished)
        return prebuiltSpeedNotification
            .setContentTitle(title)
            .setContentText(content)
            .setSubText(state)
            .setProgress(0, 0, false)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setSilent(false)
            .clearActions()
            .addAction(closeAction)
            .build()
    }

    private fun getCloseAction(): NotificationCompat.Action {
        val intent = Intent(context, CloudUpdateService::class.java).apply { action = CloudUpdateService.COMMAND_STOP }
        val pIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Action.Builder(null, context.getString(R.string.close), pIntent).build()
    }


}