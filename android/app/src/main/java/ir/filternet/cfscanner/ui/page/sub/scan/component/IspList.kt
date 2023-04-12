package ir.filternet.cfscanner.ui.page.sub.scan.component

import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.filternet.cfscanner.model.ISP
import ir.filternet.cfscanner.ui.theme.Gray
import ir.filternet.cfscanner.ui.theme.Orange
import ir.filternet.cfscanner.utils.parseToCommonName


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IspRow(configs: List<ISP>, click: (ISP) -> Unit = {}) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val activeItem = configs.none { it.active }
    Row(verticalAlignment = Alignment.CenterVertically) {
        ScreenIndicator()
        LazyRow(
            Modifier.fillMaxWidth(1f),
            horizontalArrangement = Arrangement.Start,
            state = listState,
            contentPadding = PaddingValues(horizontal = 15.dp),
            content = {
                items(configs, key = { it.uid }) {
                    val borderColor = if (it.active || it.selected) Orange else Gray
                    val backgroundColor = if (it.active) Orange else Color.Transparent
                    val textColor = when {
                        it.active -> MaterialTheme.colors.onBackground
                        it.selected -> MaterialTheme.colors.primary
                        else -> Gray
                    }

                    Text(
                        text = it.parseToCommonName(context),
                        Modifier
                            .padding(horizontal = 6.dp)
                            .background(backgroundColor, RoundedCornerShape(50))
                            .border(1.7.dp, borderColor, RoundedCornerShape(50))
                            .clip(RoundedCornerShape(50))
                            .clickable { click(it) }
                            .padding(vertical = 5.dp, horizontal = 15.dp)
                            .animateItemPlacement(tween(durationMillis = 500)),
                        style = LocalTextStyle.current.copy(fontSize = 20.sp, color = textColor)
                    )
                }
            }
        )
    }

    LaunchedEffect(activeItem) {
        if (!activeItem)
            listState.animateScrollToItem(0)
    }

}

@Composable
private fun ScreenIndicator() {
    Row(
        Modifier
            .width(35.dp)
            .background(Orange, RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
//        Image(Icons.Rounded.Wifi, contentDescription = "Configs", colorFilter = ColorFilter.tint(MaterialTheme.colors.background))
    }

}