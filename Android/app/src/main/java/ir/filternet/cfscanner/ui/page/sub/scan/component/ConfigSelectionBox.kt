package ir.filternet.cfscanner.ui.page.sub.scan.component

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.filternet.cfscanner.model.Config
import ir.filternet.cfscanner.ui.theme.Gray
import ir.filternet.cfscanner.ui.theme.Red
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun ConfigSelectionBox(
    config: List<Config> = emptyList(),
    onSelect: (Config) -> Unit = {},
    onEdit: (Config) -> Unit = {},
    onAddConfig: (String) -> Unit = {},
) {
    BoxWithConstraints(
        Modifier.fillMaxSize()
    ) {

        val padding = constraints.minHeight * 0.04f
        val listState = rememberLazyListState()
        var openConfigImport by remember(config.size) { mutableStateOf(config.isEmpty()) }
        val animate by animateFloatAsState(if (openConfigImport) 0f else 1f)

        AnimatedVisibility(
            visible = !openConfigImport,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(tween(100))
        ) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = Dp(padding), bottom = Dp(padding * 2f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                state = listState
            ) {
                items(config, { it.uid }) { config ->
                    ConfigItem(config, selected = config.selected, {
                        onEdit(config)
                    }, {
                        onSelect(config)
                    })
                }
            }
        }


        AnimatedVisibility(
            visible = openConfigImport,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(tween(100))
        ) {
            ImportScreen(Modifier.padding(top = Dp(padding * 1.5f)), isFirstTime = config.isEmpty()) {
                onAddConfig(it)
            }
        }



        Spacer(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(Dp(padding * 1.5f))
                .background(Brush.verticalGradient(0f to MaterialTheme.colors.background, 1f to Color.Transparent))
        )

        Spacer(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(Dp(padding * 2))
                .background(Brush.verticalGradient(0f to Color.Transparent, 1f to MaterialTheme.colors.background))
        )



        if (!config.isEmpty())
            AddNewConfigItem(Modifier.align(Alignment.TopCenter), rotation = animate) {
                openConfigImport = !openConfigImport
            }


    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConfigItem(config: Config, selected: Boolean = false, edit: () -> Unit = {}, clicked: () -> Unit = {}) {
    val color = if (selected) MaterialTheme.colors.primary else Gray
    Box(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(vertical = 5.5.dp, horizontal = 10.dp)
            .height(60.dp)
            .combinedClickable(
                onLongClick = { edit() },
                onClick = { clicked() }
            )
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .border(BorderStroke(2.dp, color), RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {

            RadioButton(
                selected = selected,
                onClick = { clicked() },
                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colors.primary, unselectedColor = Gray)
            )

            Column(
                Modifier
                    .fillMaxHeight()
                    .padding(vertical = 4.dp)
                    .weight(1f), verticalArrangement = Arrangement.Center
            ) {
                Text(text = config.name, color = color, fontWeight = FontWeight.Bold)
                Text(text = config.config.split("@").lastOrNull() ?: config.config, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}