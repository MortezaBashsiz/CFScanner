package ir.filternet.cfscanner.ui.page.main.scan.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import ir.filternet.cfscanner.R
import ir.filternet.cfscanner.model.ScanButtonState
import ir.filternet.cfscanner.ui.theme.Gray

@Composable
fun BoxScope.ScanButton(state: ScanButtonState, click: () -> Unit = {}) {


    val visible = state !is ScanButtonState.Disabled

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { 2 * it },
        exit = slideOutVertically { 2 * it },
        modifier = Modifier
            .padding(bottom = 75.dp)
            .fillMaxWidth(0.5f)
            .align(Alignment.BottomCenter)
    ) {

        val buttonText = when (state) {
            is ScanButtonState.Ready, is ScanButtonState.Paused -> stringResource(id = R.string.start_scan)
            is ScanButtonState.Scanning -> stringResource(id = R.string.pause_scan)
            is ScanButtonState.WaitingForNetwork -> stringResource(id = R.string.waiting_for_network)
            is ScanButtonState.Disabled -> ""
        }


        val isDeactivated = state is ScanButtonState.WaitingForNetwork || state is ScanButtonState.Disabled
        val buttonColorAnimated by animateColorAsState(targetValue = if (isDeactivated) Gray else MaterialTheme.colors.primary, tween(400))
        val buttonTextColorAnimated by animateColorAsState(targetValue = if (isDeactivated) Color.White else MaterialTheme.colors.background , tween(250))
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.connecting))

        Box(
            Modifier
                .fillMaxWidth(0.2f)
                .height((45 + 22.5).dp)
        ) {

            Card(
                Modifier
                    .height(45.dp)
                    .align(Alignment.BottomCenter),
                shape = RoundedCornerShape(50),
                backgroundColor = buttonColorAnimated,
                elevation = 7.dp,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(!isDeactivated) { click() }, contentAlignment = Alignment.Center
                ) {

                    Text(text = buttonText, style = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold), color =buttonTextColorAnimated)
                }
            }

            AnimatedVisibility(
                visible = state is ScanButtonState.WaitingForNetwork,
                modifier =Modifier
                    .size(45.dp)
                    .align(Alignment.TopCenter),
                enter = slideInVertically{ it/2} + fadeIn(),
                exit = slideOutVertically{it/2}+ fadeOut(tween(150))
            ) {
                Box() {
                    Canvas(modifier = Modifier.fillMaxSize(0.85f).align(Alignment.Center)) {
                        drawArc(buttonColorAnimated, 0f, -180f, true)
                    }

                    Column(Modifier.align(Alignment.TopCenter)) {
                        LottieAnimation(composition, restartOnPlay = true, iterations = Int.MAX_VALUE)
                        Spacer(modifier = Modifier.height(90.dp))
                    }

                }
            }

        }

    }
}