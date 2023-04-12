package ir.filternet.cfscanner.ui.page.sub.scan.component

import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.filternet.cfscanner.model.Config
import ir.filternet.cfscanner.ui.theme.Gray
import ir.filternet.cfscanner.ui.theme.Orange


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConfigRow(configs: List<Config>,editConfig: (Config) -> Unit = {}, newConfig: () -> Unit = {}, click: (Config) -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ScreenIndicator()
        LazyRow(
            Modifier.fillMaxWidth(1f),
            horizontalArrangement = Arrangement.Start,
            contentPadding = PaddingValues(horizontal = 15.dp),
            content = {
                items(configs, key = { it.uid }) { config ->
                    val borderColor = if (config.active || config.selected) Orange else Gray
                    val backgroundColor = if (config.active) Orange else Color.Transparent
                    val textColor = when {
                        config.active -> MaterialTheme.colors.onBackground
                        config.selected -> MaterialTheme.colors.primary
                        else -> Gray
                    }
                    Text(
                        text = config.name,
                        Modifier
                            .padding(horizontal = 6.dp)
                            .background(backgroundColor, RoundedCornerShape(50))
                            .border(1.7.dp, borderColor, RoundedCornerShape(50))
                            .clip(RoundedCornerShape(50))
//                            .clickable { click(config) }
                            .pointerInput(Unit){
                                detectTapGestures(
                                    onLongPress = {
                                        editConfig(config)
                                    },
                                    onTap = {
                                        click(config)
                                    }
                                )
                            }
                            .padding(vertical = 5.dp, horizontal = 15.dp)
                            .animateItemPlacement(tween(durationMillis = 350)),
                        style = LocalTextStyle.current.copy(fontSize = 20.sp, color = textColor)
                    )
                }

                item {
//                    AddItem(newConfig)
                }

            }
        )
    }
}



@Composable
private fun ScreenIndicator() {
    Row(
        Modifier
            .width(35.dp)
            .background(Orange, RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
            .padding(vertical =6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
//        Image(Icons.Rounded.SettingsInputComponent, contentDescription = "Configs", colorFilter = ColorFilter.tint(MaterialTheme.colors.background))
    }

}