package ir.filternet.cfscanner.ui.page.main.scan.component

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GroupWork
import androidx.compose.material.icons.rounded.TripOrigin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.filternet.cfscanner.model.Log
import ir.filternet.cfscanner.model.STATUS.*
import ir.filternet.cfscanner.ui.theme.Gray
import ir.filternet.cfscanner.ui.theme.Green
import ir.filternet.cfscanner.ui.theme.Orange
import ir.filternet.cfscanner.ui.theme.Red

@Composable
fun LogBox(modifier: Modifier = Modifier, log: List<Log>? = emptyList()) {

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val state = rememberLazyListState()

        LazyColumn(
            modifier.padding(horizontal = 38.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
            reverseLayout = true,
            userScrollEnabled = false,
            state = state
        ) {
            if (log != null)
                items(log, key = { it.uid }) {
                    LogItem(it)
                }
        }

        Spacer(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight()
                .background(MaterialTheme.colors.background.copy(0.5f))
        )

        Spacer(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight()
                .background(Brush.verticalGradient(0f to Color.Transparent, 1f to MaterialTheme.colors.background))
        )

        LaunchedEffect(log) {
            val index = if (log?.lastIndex == null || log.lastIndex < 0) 0 else log.lastIndex
            state.scrollToItem(index, 0)
        }
    }

}

@Composable
fun LogItem(log: Log) {
    val icon = when (log.status) {
        IDLE -> Icons.Rounded.TripOrigin
        INPROGRESS -> Icons.Rounded.GroupWork  // Icons.Rounded.OfflineBolt //Icons.Rounded.Adjust
        FAILED -> Icons.Rounded.Cancel
        SUCCESS -> Icons.Rounded.CheckCircle
    }

    val color = when (log.status) {
        IDLE -> Gray
        INPROGRESS -> Orange
        FAILED -> Red
        SUCCESS -> Green
    }

    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        )
    )

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.animateContentSize()) {
        Image(icon, contentDescription = "",
            Modifier
                .size(15.dp)
                .rotate(if (log.status == INPROGRESS) angle else 0f), colorFilter = ColorFilter.tint(color))
        Text(text = log.text, Modifier.padding(horizontal = 4.dp), fontSize = 14.sp, color = color)
    }
}