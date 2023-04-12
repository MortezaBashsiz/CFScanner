package ir.filternet.cfscanner.ui.page.main.components

import androidx.annotation.IntegerRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.round
import kotlin.math.roundToInt

@Composable
fun PlayfulBottomNavigation(
    modifier: Modifier = Modifier,
    @IntegerRes icons: Array<Int>,
    index: Int = 0,
    duration: Int = 1200,
    unselectedColor: Color = Color.White,
    selectedColor: Color = MaterialTheme.colors.primary,
    indicatorColor: Color = MaterialTheme.colors.primary,
    backgroundColor:Color = MaterialTheme.colors.primary,
    navigate: (index: Int) -> Unit
) {
    Box(modifier = modifier.background(backgroundColor, RoundedCornerShape(topStartPercent = 15, topEndPercent = 15)).padding(horizontal = 16.dp).height(65.dp)) {
        val ANIM_DURATION = duration
        var currentIndex by remember(index) { mutableStateOf(index * 1f) }
        var show by rememberSaveable { mutableStateOf(true) }
        val upcome by animateFloatAsState(if (show) 0f else 1f, tween(ANIM_DURATION))

        Layout(
            modifier = Modifier.padding(bottom = 5.dp),
            content = {
                repeat(icons.size) {
                    Icon(
                        painter = painterResource(id = icons[it]),
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
//                            .background(iconColor.copy(0.2f), RoundedCornerShape(50))
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                currentIndex = it * 1f
                                navigate(it)
                            }
                            .padding(6.dp),
                        tint = if(it == currentIndex.roundToInt()) selectedColor else unselectedColor
                    )
                }
            }
        ) { measurables, constraints ->
            val maxH = constraints.maxHeight
            val maxW = constraints.maxWidth
            val size = constraints.copy(minWidth = 0, minHeight = 0)
            val children = measurables.map { it.measure(size) }
            val spacer = (maxW.toFloat() / icons.size).div(2f) - (children.minOf { it.width } / 2f)

            fun leverage(index: Int, max: Float, min: Float, count: Int): Float {
                val x = ((count - 1) / 2)
                return round(((((max - min) / x) * (Math.abs((index - x))) * -1) + max) * 100) / 100
            }

            layout(constraints.maxWidth, constraints.maxHeight) {
                var space = spacer+1
                children.forEachIndexed { i, d ->
                    val h = ((maxH / 2) + ((maxH) * (1 - ((upcome * (leverage(i, 0.4f, 0.1f, children.size) * 10)).coerceIn(0f, 1f))))) - (d.height / 2)
                    d.place(space.toInt(), h.toInt())
                    if(i == children.size-1) return@layout
                    space += (d.width.toFloat() + (spacer * 2f))
                }
            }
        }




        val anim by animateFloatAsState(targetValue = currentIndex, spring())
        val count = icons.size
        Canvas(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .height(19.dp)
                .fillMaxWidth()
        ) {
            val w = this.size.width
            val h = this.size.height
            val size = 10f

            val spacer = (w / (count * 2))
            drawCircle(if (upcome == 1f) indicatorColor else Color.Transparent, radius = size, Offset(((anim * 2f) + 1) * spacer, h / 2f))
            drawCircle(if (upcome == 1f) indicatorColor.copy(0.3f) else Color.Transparent, radius = size + 5, Offset(((anim * 2f) + 1) * spacer, h / 2f))

        }

        LaunchedEffect(show) {
            if(show){
                delay(500)
                show = false
            }
        }
    }
}


