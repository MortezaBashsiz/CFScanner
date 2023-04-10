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

class SpeedNotificationManager constructor(@ApplicationContext private val context: Context) : NotificationManager(context) {

    private var contentIntent: PendingIntent? = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private val SPEED_CHANNEL_ID = "SPEED_CHANNEL"
    private val speedChannel = NotificationChannelCompat.Builder(SPEED_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
        .setName(context.getString(R.string.speed_channel))
        .setDescription(context.getString(R.string.speed_channel_description))
        .build()

    private val prebuiltSpeedNotification = NotificationCompat.Builder(context, SPEED_CHANNEL_ID)
        .setSmallIcon(R.drawable.cf_logo)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)

    init {
        notificationManager.createNotificationChannel(speedChannel)
    }

    private val closeAction = getCloseAction()

    fun getSpeedCheckingNotification(remainItems: Int, remainSeconds: Long, scanId: Int): Notification {
        val state = context.getString(R.string.ongoing)
        val estimatedTime = remainSeconds.let {
            when {
                it < 60 -> (remainSeconds).toString() +" "+ context.getString(R.string.sec)
                it < 60*60 -> (remainSeconds / 60).toString() +" "+ context.getString(R.string.min)
                else -> (remainSeconds / 3600).toString() +" "+ context.getString(R.string.h)
            }
        }
        val title = context.getString(R.string.checking_title)
        val content = context.getString(R.string.checking_connections_speed, remainItems, estimatedTime)
        val rawIntent = Intent(Intent.ACTION_VIEW, "cfscanner://scandetails/?id=$scanId".toUri())
        val contentIntent = PendingIntent.getActivity(context, 0, rawIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return prebuiltSpeedNotification
            .setContentTitle(title)
            .setContentText(content)
            .setSubText(state)
            .setContentIntent(contentIntent)
            .setSound(null)
            .setOngoing(true)
            .setSilent(true)
            .clearActions()
            .addAction(closeAction)
            .build()
    }

    fun getSpeedCheckFinishedNotification(scanId: Int): Notification {
        val state = context.getString(R.string.finished)
        val content = context.getString(R.string.speed_checking_finished)
        val title = context.getString(R.string.done)
        val rawIntent = Intent(Intent.ACTION_VIEW, "cfscanner://scandetails/?id=$scanId".toUri())
        val contentIntent = PendingIntent.getActivity(context, 0, rawIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return prebuiltSpeedNotification
            .setContentTitle(title)
            .setContentText(content)
            .setSubText(state)
            .setContentIntent(contentIntent)
            .setOngoing(false)
            .setSilent(false)
            .clearActions()
            .build()
    }

    private fun getCloseAction(): NotificationCompat.Action {
        val intent = Intent(context, CloudSpeedService::class.java).apply { action = CloudSpeedService.COMMAND_STOP }
        val pIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Action.Builder(null, context.getString(R.string.close), pIntent).build()
    }


}