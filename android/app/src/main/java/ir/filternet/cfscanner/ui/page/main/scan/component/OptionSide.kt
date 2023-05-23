package ir.filternet.cfscanner.ui.page.main.scan.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import ir.filternet.cfscanner.R
import ir.filternet.cfscanner.utils.clickableWithNoRipple
import ir.filternet.cfscanner.utils.isRTL
import kotlinx.coroutines.delay

@Composable
fun BoxScope.OptionSide(onClick:()->Unit = {}) {

    var show by remember{ mutableStateOf(false) }
    val isRTL = isRTL()

    AnimatedVisibility(
        visible = show,
        enter = slideInHorizontally(tween(500)){ ((if(isRTL) -1 else 1 )*it)/2}+ fadeIn(),
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .height(175.dp)
    ) {
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .height(175.dp)
        ) {
            Image(
                painter = rememberAsyncImagePainter(R.drawable.side_button),
                contentDescription = stringResource(R.string.skip_this_range),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickableWithNoRipple { onClick() }
                    .height(175.dp)
            )

            Text(
                text = stringResource(R.string.skip_this_range),
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp)
                    .vertical()
                    .rotate(-90f)
            )
        }
    }

    LaunchedEffect(Unit){
        delay(700)
        show = true
    }

}

fun Modifier.vertical() =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.height, placeable.width) {
            placeable.place(
                x = -(placeable.width / 2 - placeable.height / 2),
                y = -(placeable.height / 2 - placeable.width / 2)
            )
        }
    }
