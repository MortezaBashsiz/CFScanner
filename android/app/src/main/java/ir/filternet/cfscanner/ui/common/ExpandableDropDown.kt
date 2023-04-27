package ir.filternet.cfscanner.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import dev.chrisbanes.snapper.ExperimentalSnapperApi
import dev.chrisbanes.snapper.rememberSnapperFlingBehavior
import ir.filternet.cfscanner.utils.clickableWithNoRipple
import ir.filternet.cfscanner.utils.findIndex
import ir.filternet.cfscanner.utils.percep
import ir.filternet.cfscanner.utils.pxToDp
import ir.filternet.cfscanner.utils.toPx
import kotlinx.coroutines.delay


@Composable
fun <E> ExpandableDropDown(
    items: List<E>,
    modifier: Modifier = Modifier,
    showPop:Boolean = false,
    convert: (item: E) -> String = { it.toString() },
    selected: Int = 0,
    holderOffset: Int = 1,
    enabled: Boolean = true,
    onSelect: (item: E) -> Unit
) {


    var show by remember(showPop) { mutableStateOf(showPop) }
    val animate by animateFloatAsState(targetValue = if (show) 1f else 0f, animationSpec = tween(500))
    var selectedItem by remember(selected,items){ mutableStateOf(items[selected % items.size])}


    Box(
        modifier
            .clickableWithNoRipple {
                if (enabled)
                    show = !show
            },
        contentAlignment = Alignment.Center
    ) {



        AnimatedVisibility(visible = animate > 0f, enter = fadeIn(), exit = fadeOut()) {
            ExpandableDropDownPopup(
                items = items,
                modifier = Modifier.fillMaxSize(),
                convert = convert,
                selected = items.findIndex { it ==selectedItem },
                dismiss = { show = false },
                offset = animate,
                blockOffset = holderOffset,
                select = {
                    selectedItem = it
                }
            )
        }

        Row(
            modifier = Modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {

            if (animate < 1f)
                ExpandableDropDownItem(
                    convert(selectedItem),
                    Modifier.fillMaxHeight(),
                    alpha = if (enabled) 1f else 0.5f,
                    LocalTextStyle.current.color,
                ) {
                    if (enabled) {
                        show = !show
                    }
                }
//            Spacer(modifier = Modifier.weight(1f))
//            val angle = if (LocalLayoutDirection.current == LayoutDirection.Rtl) +90f else -90f
//            val icon = if (LocalLayoutDirection.current == LayoutDirection.Rtl) Icons.Rounded.ChevronRight else Icons.Rounded.ChevronLeft
//            Icon(icon, contentDescription = null, modifier = Modifier.rotate((1 - animate) * angle))
        }

        LaunchedEffect(selectedItem){
            delay(500)
            onSelect(selectedItem)
        }

    }
}

@Composable
private fun ExpandableDropDownItem(text: String, modifier: Modifier, alpha: Float = 1f,color: Color = Color.Unspecified, click: () -> Unit = {}) {
    Box(
        modifier = modifier.clickableWithNoRipple { click() },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            color = color.copy(alpha),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun <E> ExpandableDropDownPopup(
    items: List<E>,
    modifier: Modifier,
    convert: (item: E) -> String,
    selected: Int = 0,
    offset: Float = 0f,
    blockOffset: Int = 1,
    dismiss: () -> Unit,
    select: (item: E) -> Unit
) {

    val colorAnimate by animateColorAsState(targetValue = if (offset < 0.3f) MaterialTheme.colors.background else MaterialTheme.colors.onSurface, tween(300))
    var preSelected by remember { mutableStateOf(null as E?) }


    if (offset > 0.0f)
        BoxWithConstraints(
            modifier
                .fillMaxSize()
        ) {
            val h = this.minHeight.toPx().toInt()
            val w = this.maxWidth

            val popupBlockOffset = -(h * (blockOffset * 2)) - (h / 2)
            val holderH = ((((blockOffset * 2) * 2) + 2) * h)
            val desireW = w /*(w - 20.dp)*/
            val growH = h + ((((blockOffset * 2) + 1) * h) * offset)
            val width = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -w.toPx().toInt() else 0
            Popup(
                offset = IntOffset(width, popupBlockOffset),
                onDismissRequest = {
                    if (preSelected != null)
                        select(preSelected!!)

                    dismiss()
                }
            ) {

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(
                        modifier = Modifier
                            .height(holderH.pxToDp()),
                        contentAlignment = Alignment.CenterStart
                    ) {


                        Card(
                            modifier = Modifier
                                .height(
                                    growH
                                        .toInt()
                                        .pxToDp()
                                )
                                .alpha(percep(0.2f, 1f, offset))
                                .width(desireW),
                            elevation = (offset * 2).dp,
                            shape = RoundedCornerShape((4 * offset).dp),
                            backgroundColor = colorAnimate
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val alpha = (offset - 0.8f).coerceAtLeast(0f)
                                Icon(
                                    Icons.Rounded.ChevronLeft, contentDescription = null, modifier = Modifier
                                        .size((h / 2).pxToDp())
                                        .rotate(+90f)
                                        .alpha(alpha)
                                )


                                if (offset == 1f)
                                    ExpandableDropDownItemList(
                                        items,
                                        Modifier.weight(1f),
                                        Modifier
                                            .height(h.pxToDp())
                                            .width(desireW)
                                            .alpha(offset),
                                        convert,
                                        selected - blockOffset,
                                        blockOffset,
                                        {
                                            preSelected = it
                                        }
                                    ) {
                                        preSelected = null
                                        select(it)
                                        dismiss()
                                    }
                                else
                                    Spacer(modifier = Modifier.weight(1f))


                                Icon(
                                    Icons.Rounded.ChevronLeft, contentDescription = null, modifier = Modifier
                                        .size((h / 2).pxToDp())
                                        .rotate(-90f)
                                        .alpha(alpha)
                                )
                            }
                        }


                        if (offset < 1f)
                            ExpandableDropDownItem(
                                convert(items[selected % items.size]),
                                Modifier
                                    .height(h.pxToDp())
                                    .width(desireW)
                            )


                        Spacer(
                            modifier = Modifier
                                .height(h.pxToDp())
                                .width(w)
                        )

                    }
                }

            }

        }


}


@OptIn(ExperimentalSnapperApi::class)
@Composable
private fun <G> ExpandableDropDownItemList(
    items: List<G>,
    modifierList: Modifier,
    modifierItem: Modifier,
    convert: (item: G) -> String,
    selectedPosition: Int = 0,
    blockOffset: Int = 1,
    preSelect: (data: G) -> Unit,
    selected: (data: G) -> Unit
) {
    val maxSize = (Int.MAX_VALUE / 2)
    val offset = maxSize % items.size
    val state = rememberLazyListState(maxSize - offset + selectedPosition)


    @Composable
    fun alphair(index: Int): Float {
        val centerIndex = state.firstVisibleItemIndex + blockOffset
        val offset = (45.dp.toPx() - state.firstVisibleItemScrollOffset) / 45.dp.toPx()
        return when (index) {
            centerIndex -> (0.5f) + (0.5f * offset)
            centerIndex + 1 -> (0.5f) + (0.5f * (1 - offset))
            else -> 0.5f
        }
    }


    LazyColumn(
        modifierList, state = state,
        flingBehavior = rememberSnapperFlingBehavior(state),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(Int.MAX_VALUE) { index ->
            val element = items[index % items.size]
            ExpandableDropDownItem(
                convert(element!!),
                modifierItem,
                alpha = alphair(index),
                MaterialTheme.colors.onBackground
            ) {
                selected(element)
            }
        }
    }

    LaunchedEffect(state.firstVisibleItemIndex) {
        val index = ((((state.firstVisibleItemIndex) % items.size) + blockOffset) % items.size)
        preSelect(items[index])
    }


}
