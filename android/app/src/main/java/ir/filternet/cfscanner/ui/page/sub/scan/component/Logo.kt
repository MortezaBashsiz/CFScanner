package ir.filternet.cfscanner.ui.page.sub.scan.component

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import ir.filternet.cfscanner.R


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CloudLogo(loading: Boolean = false,click: () -> Unit = {}) {
    val infinitAnim = rememberInfiniteTransition()
    val value by infinitAnim.animateFloat(
        initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val notifPermissionState = rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)

    BoxWithConstraints(
        Modifier
            .fillMaxWidth(0.5f), contentAlignment = Alignment.TopCenter
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = rememberAsyncImagePainter(
                    if (isSystemInDarkTheme())
                        R.drawable.cf_logo_text_dark
                    else
                        R.drawable.cf_logo_text_light
                ), contentDescription = ""
            )
            Spacer(modifier = Modifier.height(22.dp))
        }

        AnimatedVisibility(visible = loading,Modifier.align(Alignment.BottomCenter)) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier
                        .fillMaxWidth()) {
                    Spacer(modifier = Modifier.weight(value.coerceAtLeast(0.0001f)))
                    Image(
                        painter = rememberAsyncImagePainter(R.drawable.scanner), modifier = Modifier
                            .width(45.dp)
                            .rotate(0 + (90f * value)),
                        contentDescription = ""
                    )
                    Spacer(modifier = Modifier.weight((1f - value).coerceAtLeast(0.0001f)))
                }
                Spacer(modifier = Modifier.height(22.dp))
            }

        }
    }

    LaunchedEffect(Unit){
        notifPermissionState.launchPermissionRequest()
    }
}