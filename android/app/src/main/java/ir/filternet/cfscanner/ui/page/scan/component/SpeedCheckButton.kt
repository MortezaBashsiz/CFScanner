package ir.filternet.cfscanner.ui.page.scan.component

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.filternet.cfscanner.R
import ir.filternet.cfscanner.service.CloudSpeedService
import ir.filternet.cfscanner.ui.theme.Gray
import ir.filternet.cfscanner.ui.theme.Green

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpeedCheckButton(speedStatus: CloudSpeedService.SpeedServiceStatus, startSort: () -> Unit = {}, stopSorting: () -> Unit = {}) {

    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        )
    )

    val color = when (speedStatus) {
        is CloudSpeedService.SpeedServiceStatus.Checking -> Green
        is CloudSpeedService.SpeedServiceStatus.Disable -> Gray
        is CloudSpeedService.SpeedServiceStatus.Idle -> MaterialTheme.colors.primary
    }

    val text = when (speedStatus) {
        is CloudSpeedService.SpeedServiceStatus.Checking -> stringResource(id = R.string.checking, (speedStatus.progress * 100).toInt())
        is CloudSpeedService.SpeedServiceStatus.Disable -> stringResource(id = R.string.sort_by_speed)
        is CloudSpeedService.SpeedServiceStatus.Idle -> stringResource(id = R.string.sort_by_speed)
    }

    Card(
        Modifier
            .width(150.dp)
            .height(40.dp),
        backgroundColor = color,
        shape = RoundedCornerShape(50),
        elevation = 5.dp
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .combinedClickable(
                    onClick = {
                        if (speedStatus is CloudSpeedService.SpeedServiceStatus.Idle)
                            startSort()
                    },
                    onLongClick = {
                        if (speedStatus is CloudSpeedService.SpeedServiceStatus.Checking)
                            stopSorting()
                    }
                )
                .padding(5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = text, fontSize = 15.5.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)

            if (speedStatus is CloudSpeedService.SpeedServiceStatus.Checking)
                Icon(Icons.Rounded.Sync, contentDescription = "Sync", modifier = Modifier.rotate(angle))
        }
    }

    Row(
        Modifier
            .height(30.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = speedStatus is CloudSpeedService.SpeedServiceStatus.Checking,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "Hold button to stop speed checking process",
                fontSize = 11.sp,
                fontWeight = FontWeight.Thin,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .alpha(0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}