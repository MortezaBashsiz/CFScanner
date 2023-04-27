package ir.filternet.cfscanner.ui.page.sub.cidr_management

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.filternet.cfscanner.R
import ir.filternet.cfscanner.contracts.SIDE_EFFECTS_KEY
import ir.filternet.cfscanner.model.CIDR
import ir.filternet.cfscanner.ui.common.HeaderPage
import ir.filternet.cfscanner.ui.page.main.scan.ScanContract
import ir.filternet.cfscanner.ui.page.main.scan.component.LoadingView
import ir.filternet.cfscanner.ui.theme.Gray
import ir.filternet.cfscanner.ui.theme.Green
import ir.filternet.cfscanner.ui.theme.Red
import ir.filternet.cfscanner.utils.extractValidAddress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import org.burnoutcrew.reorderable.*
import timber.log.Timber

@Composable
fun CidrManagementScreen(
    state: CidrManagementContract.State,
    effectFlow: Flow<CidrManagementContract.Effect>?,
    onEventSent: (event: CidrManagementContract.Event) -> Unit,
    onNavigationRequested: (navigationEffect: CidrManagementContract.Effect.Navigation) -> Unit,
) {

    val context = LocalContext.current

    LaunchedEffect(SIDE_EFFECTS_KEY) {
        effectFlow?.onEach { effect ->
            when (effect) {
                is CidrManagementContract.Effect.Messenger.Toast -> {
                    val message = effect.message ?: context.getString(effect.messageId!!)
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }

                is CidrManagementContract.Effect.Navigation.NavigateUP -> {
                    onNavigationRequested.invoke(effect)
                }
            }
        }?.collect()
    }


    val loading = state.loading
    val data = state.cidrs
    var autoFetchEnabled = state.autofetch
    var shuffleEnabled = state.shuffle
    var customRange = state.customRange
    val notDraggableItems = 6

    val listDragState = rememberReorderableLazyListState(
        canDragOver = { from, to ->
            (from.index > (notDraggableItems - 1) && to.index > (notDraggableItems - 1))
        },
        onDragEnd = { start, end ->
            Timber.d("Drag End from ${start - 2} to ${end - 2}")
            if (start != end)
                onEventSent.invoke(CidrManagementContract.Event.SaveCidrs)
        },
        onMove = { from, to ->

            Timber.d("Change Order from ${from.index - 2} to ${to.index - 2}")
            data.apply {
                if (from.index <= (notDraggableItems - 1) && to.index <= (notDraggableItems - 1)) return@apply
                val from = from.index - notDraggableItems
                val to = to.index - notDraggableItems
                onEventSent.invoke(CidrManagementContract.Event.MoveCidr(from, to))
            }
        })



    LazyColumn(
        state = listDragState.listState,
        modifier = Modifier
            .fillMaxSize()
            .reorderable(listDragState)
            .detectReorderAfterLongPress(listDragState),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(bottom = 10.dp, end = 10.dp,start = 10.dp)
    ) {

        item {
            HeaderPage(stringResource(id = R.string.manage_cidrs)){
                onNavigationRequested.invoke(CidrManagementContract.Effect.Navigation.NavigateUP)
            }
        }

        item {
            AutoFetch(autoFetchEnabled) {
                autoFetchEnabled = it
                onEventSent.invoke(CidrManagementContract.Event.AutoFetchChange(it))
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            ShuffleList(shuffleEnabled) {
                onEventSent.invoke(CidrManagementContract.Event.ShuffleChange(it))
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            CustomRange(customRange) {
                onEventSent.invoke(CidrManagementContract.Event.CustomRange(it))
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            CustomRangeImport {
                onEventSent.invoke(CidrManagementContract.Event.AddIpRanges(it))
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        if (loading)
            item {
                LoadingView()
            }

        if (!shuffleEnabled && loading.not())
            item {
                Text(
                    text = stringResource(R.string.hold_item_to_reorder),
                    color = Gray.copy(0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

        if (!shuffleEnabled)
            items(data, { it.uid }) { item ->
                Spacer(modifier = Modifier.height(4.dp))
                ReorderableItem(listDragState, key = item.uid) { isDragging ->
                    val elevation = animateDpAsState(if (isDragging) 16.dp else 0.dp)
                    Column(
                        modifier = Modifier
                            .shadow(elevation.value)
                            .background(MaterialTheme.colors.surface)
                    ) {

                        IpRangeList(item, isDragging) {
                            onEventSent.invoke(CidrManagementContract.Event.RemoveCIDR(item))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
    }
}


@Composable
private fun AutoFetch(state: Boolean, onChange: (Boolean) -> Unit = {}) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.onSurface, RoundedCornerShape(5))
            .padding(10.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.auto_fetch_ip_range), Modifier.weight(1f))
            Switch(
                checked = state,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Green,
                    uncheckedThumbColor = Gray,
                    checkedTrackColor = Green.copy(0.4f),
                    uncheckedTrackColor = Gray.copy(0.4f)
                )
            )
        }

        Text(text = stringResource(R.string.auto_fetch_ip_range_desc), modifier = Modifier.padding(4.dp), fontSize = 13.sp, fontWeight = FontWeight.Light)
    }

}

@Composable
private fun CustomRange(state: Boolean, onChange: (Boolean) -> Unit = {}) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.onSurface, RoundedCornerShape(5))
            .padding(10.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.custom_range), Modifier.weight(1f))
            Switch(
                checked = state,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Green,
                    uncheckedThumbColor = Gray,
                    checkedTrackColor = Green.copy(0.4f),
                    uncheckedTrackColor = Gray.copy(0.4f)
                )
            )
        }

        Text(text = stringResource(R.string.custom_range_desc), modifier = Modifier.padding(4.dp), fontSize = 13.sp, fontWeight = FontWeight.Light)
    }

}

@Composable
private fun ShuffleList(state: Boolean, onChange: (Boolean) -> Unit = {}) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.onSurface, RoundedCornerShape(5))
            .padding(10.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.shuffle_ip_range), Modifier.weight(1f))
            Switch(
                checked = state,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Green,
                    uncheckedThumbColor = Gray,
                    checkedTrackColor = Green.copy(0.4f),
                    uncheckedTrackColor = Gray.copy(0.4f)
                )
            )
        }

        Text(text = stringResource(R.string.shuffle_ip_range_desc), modifier = Modifier.padding(4.dp), fontSize = 13.sp, fontWeight = FontWeight.Light)
    }

}

@Composable
private fun CustomRangeImport(ips: (List<String>) -> Unit) {
    var text by remember { mutableStateOf("") }
    var inputedIpCount by remember { mutableStateOf(0) }
    Text(
        text = stringResource(R.string.enter_ip_range),
        modifier = Modifier.fillMaxWidth(0.9f),
    )
    Spacer(modifier = Modifier.height(4.dp))
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            inputedIpCount = extractValidAddress(it).count()
        },
        modifier = Modifier.fillMaxWidth(0.9f),
        colors = TextFieldDefaults.textFieldColors(unfocusedIndicatorColor = Gray),
        placeholder = { Text(text = stringResource(R.string.ip_range_example), color = Gray.copy(0.4f)) },
        minLines = 1,
        maxLines = 5
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.for_multi_ip_enter),
        color = Gray.copy(0.4f),
        fontSize = 14.sp,
        fontWeight = FontWeight.Light,
    )
    Spacer(modifier = Modifier.height(10.dp))
    Card(
        Modifier
            .width(150.dp)
            .height(45.dp),
        backgroundColor = if (inputedIpCount > 0) Green else Gray
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(inputedIpCount > 0) {
                    ips(extractValidAddress(text))
                    text = ""
                    inputedIpCount = extractValidAddress(text).count()
                }, contentAlignment = Alignment.Center
        ) {
            val range = if (inputedIpCount > 1) "$inputedIpCount " else ""
            Text(text = stringResource(R.string.add_ip_range_button,range), color = Color.White)
        }
    }
}

@Composable
private fun IpRangeList(cidr: CIDR, dragging: Boolean = false, delete: () -> Unit) {
    val animateColor by animateColorAsState(
        if (dragging) MaterialTheme.colors.primary else Gray.copy(0.8f),
        tween(500)
    )
    Column {

        Card(
            Modifier
                .fillMaxWidth(),
            backgroundColor = MaterialTheme.colors.onSurface
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(45.dp)
                    .padding(vertical = 8.dp, horizontal = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Spacer(modifier = Modifier.width(2.dp))

                Icon(
                    Icons.Rounded.DragIndicator, contentDescription = "Drag",
                    tint = animateColor,
                    modifier = Modifier.clip(RoundedCornerShape(50))
                )

                Text(
                    text = "#${cidr.position + 1}",
                    fontWeight = FontWeight.Bold,
                    color = if (dragging) MaterialTheme.colors.onBackground else Gray.copy(0.8f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = cidr.address + " / " + cidr.subnetMask,
                    Modifier.animateContentSize()
                )

                if (cidr.custom) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "C",
                        Modifier
                            .width(25.dp)
                            .height(15.dp)
                            .background(MaterialTheme.colors.primary, RoundedCornerShape(10)),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    Icons.Rounded.Delete, contentDescription = "Delete",
                    tint = Red,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            delete()
                        }
                )

                Spacer(modifier = Modifier.width(4.dp))
            }

        }

    }

}
