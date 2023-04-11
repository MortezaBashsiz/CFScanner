package ir.filternet.cfscanner.utils

import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import ir.filternet.cfscanner.service.CloudSpeedService
import timber.log.Timber

@Composable
fun BindService(serviceClass: Class<*>,connection: ServiceConnection){
    val context = LocalContext.current
    val lifecycler = LocalLifecycleOwner.current
    DisposableEffect(lifecycler) {
        val observer = LifecycleEventObserver { _, event ->
            try {
                if (event == Lifecycle.Event.ON_START) {
                    Timber.d("BindServiceCompose onBind $serviceClass $connection")
                    context.bindService(Intent(context,serviceClass),connection, Context.BIND_AUTO_CREATE)
                } else if (event == Lifecycle.Event.ON_STOP) {
                    Timber.d("BindServiceCompose onUnbind $serviceClass $connection")
                    context.unbindService(connection)
                }
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
        lifecycler.lifecycle.addObserver(observer)
        onDispose {
            Timber.d("BindServiceCompose Dispose")
            lifecycler.lifecycle.removeObserver(observer)
        }
    }
}