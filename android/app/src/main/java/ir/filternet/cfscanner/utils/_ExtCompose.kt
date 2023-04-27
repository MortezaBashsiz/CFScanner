package ir.filternet.cfscanner.utils

import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.BeyondBoundsLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import ir.filternet.cfscanner.service.CloudSpeedService
import timber.log.Timber

@Composable
fun Dp.toPx(): Float = with(LocalDensity.current) { this@toPx.toPx() }

@Composable
fun Int.pxToDp() = with(LocalDensity.current) { this@pxToDp.toDp() }

inline fun Modifier.clickableWithNoRipple(crossinline onClick: ()->Unit): Modifier = composed {
    clickable(indication = null,
        interactionSource = remember { MutableInteractionSource() }) {
        onClick()
    }
}


@Stable
fun Modifier.mirror(): Modifier = composed {
    if (LocalLayoutDirection.current == LayoutDirection.Rtl)
        this.scale(scaleX = -1f, scaleY = 1f)
    else
        this
}

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