package ir.filternet.cfscanner.ui.page.sub.scan.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.filternet.cfscanner.R
import ir.filternet.cfscanner.model.ScanButtonState
import ir.filternet.cfscanner.ui.page.main.MainScreenVM
import ir.filternet.cfscanner.ui.theme.Orange
import ir.filternet.cfscanner.ui.theme.OrangeLight

@Composable
fun BoxScope.ScanButton(state: ScanButtonState, click: () -> Unit = {}) {


    val visible = state !is ScanButtonState.Disabled

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { 2*it },
        exit = slideOutVertically { 2*it },
        modifier = Modifier
            .padding(bottom = 75.dp)
            .fillMaxWidth(0.5f)
            .align(Alignment.BottomCenter)
    ) {

        val buttonText = when(state){
            is ScanButtonState.Ready,is ScanButtonState.Paused -> stringResource(id = R.string.start_scan)
            is ScanButtonState.Scanning -> stringResource(id = R.string.pause_scan)
            is ScanButtonState.Disabled -> ""
        }

        Card(
            Modifier.fillMaxWidth(0.2f).height(45.dp),
            shape = RoundedCornerShape(50),
            backgroundColor = MaterialTheme.colors.primary,
            elevation = 7.dp,
        ) {
            Box(modifier = Modifier.fillMaxSize().clickable { click() }, contentAlignment = Alignment.Center){
                Text(text = buttonText, style = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold))
            }
        }


    }
}