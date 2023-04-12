package ir.filternet.cfscanner.contracts

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationManagerCompat

abstract class NotificationManager(private val context: Context) {

    protected var notificationManager:NotificationManagerCompat = NotificationManagerCompat.from(context)

    @SuppressLint("MissingPermission")
    fun notify(id: Int, notif: Notification) {
        try {
            notificationManager.notify(id, notif)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun clearNotification(id: Int) {
        notificationManager.cancel(id)
    }

}