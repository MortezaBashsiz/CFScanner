package ir.filternet.cfscanner.ui.page.sub.scan_details.component

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.filternet.cfscanner.R
import ir.filternet.cfscanner.service.CloudSpeedService.SpeedServiceStatus

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpeedCheckButton(speedStatus: SpeedServiceStatus, startSort: () -> Unit = {}, stopSorting: () -> Unit = {}) {

    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        )
    )

    val color = MaterialTheme.colors.background

    val text = when (speedStatus) {
        is SpeedServiceStatus.Checking -> stringResource(id = R.string.evaluating, (speedStatus.progress * 100).toInt())
        is SpeedServiceStatus.Disable -> stringResource(id = R.string.start_evaluation)
        is SpeedServiceStatus.Idle -> stringResource(id = R.string.start_evaluation)
    }


    Row(
        Modifier
            .fillMaxSize()
            .width(150.dp)
            .height(40.dp)
            .background(color, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .combinedClickable(
                onClick = {
                    if (speedStatus is SpeedServiceStatus.Idle)
                        startSort()
                },
                onLongClick = {
                    if (speedStatus is SpeedServiceStatus.Checking)
                        stopSorting()
                }
            )
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            fontSize = 15.5.sp,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.primaryVariant,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )

        if (speedStatus is SpeedServiceStatus.Checking)
            Icon(Icons.Rounded.Sync, contentDescription = "Sync", modifier = Modifier.rotate(angle), tint = MaterialTheme.colors.primaryVariant)

    }

    Row(
        Modifier
            .height(18.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = speedStatus is SpeedServiceStatus.Checking,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = stringResource(R.string.hold_to_stop_process),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(0.8f),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.background.copy(0.7f)
            )
        }
    }
}