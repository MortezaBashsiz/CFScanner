package ir.filternet.cfscanner.ui.page.sub.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vivvvek.seeker.Seeker
import dev.vivvvek.seeker.SeekerDefaults
import dev.vivvvek.seeker.Segment
import dev.vivvvek.seeker.rememberSeekerState
import ir.filternet.cfscanner.BuildConfig
import ir.filternet.cfscanner.R
import ir.filternet.cfscanner.utils.openBrowser
import kotlinx.coroutines.flow.Flow

@Composable
fun ScanSettingScreen(
    state: ScanSettingsContract.State,
    effectFlow: Flow<ScanSettingsContract.Effect>?,
    onEventSent: (event: ScanSettingsContract.Event) -> Unit,
    onNavigationRequested: (navigationEffect: ScanSettingsContract.Effect.Navigation) -> Unit,
) {

    val context = LocalContext.current
    val scan = state.settings

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "Scanner Settings", fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(20.dp))
        WorkerCell(scan.worker) {
            onEventSent.invoke(ScanSettingsContract.Event.UpdateSettings(scan.copy(worker = it)))
        }
        Spacer(modifier = Modifier.height(15.dp))
        SpeedCell(scan.speedTestSize) {
            onEventSent.invoke(ScanSettingsContract.Event.UpdateSettings(scan.copy(speedTestSize = it)))
        }
        Spacer(modifier = Modifier.height(15.dp))
        FrontingCell(scan.fronting) {
            onEventSent.invoke(ScanSettingsContract.Event.UpdateSettings(scan.copy(fronting = it)))
        }
        Spacer(modifier = Modifier.height(15.dp))
        Text(text = BuildConfig.appName + " v" + BuildConfig.VERSION_NAME + "\n" + stringResource(id = R.string.update_desc),
            fontWeight = FontWeight.Light,
            fontSize = 12.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    context.openBrowser("https://github.com/sinadalvand/CFScanner/releases")
                }
                .padding(8.dp), textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(100.dp))
    }
}


@Composable
fun WorkerCell(num: Float, onChange: (Float) -> Unit = {}) {
    var value by remember { mutableStateOf(num) }
    val interactionSource = remember { MutableInteractionSource() }
    val isDragging by interactionSource.collectIsDraggedAsState()
    val state = rememberSeekerState()
    val gap by animateDpAsState(if (isDragging) 2.dp else 0.dp)
    val thumbRadius by animateDpAsState(if (isDragging) 10.dp else 5.dp)

    val segments = listOf(
        Segment(name = "1", start = 1f),
        Segment(name = "2", start = 2f),
        Segment(name = "3", start = 3f),
        Segment(name = "4", start = 4f),
        Segment(name = "5", start = 5f),
        Segment(name = "6", start = 6f),
        Segment(name = "7", start = 7f),
        Segment(name = "8", start = 8f),
        Segment(name = "9", start = 9f),
    )

    LaunchedEffect(isDragging) {
        if (!isDragging && num != value)
            onChange(value)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.onSurface)
            .padding(10.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.worker), Modifier.weight(1f))
            Text(text = state.currentSegment.name)
        }

        Spacer(modifier = Modifier.height(5.dp))

        Seeker(
            value = value,
            modifier = Modifier.height(20.dp),
            range = 1f..9f,
            state = state,
            segments = segments,
            interactionSource = interactionSource,
            dimensions = SeekerDefaults.seekerDimensions(gap = gap, thumbRadius = thumbRadius),
            onValueChange = { value = it }
        )

        Text(text = stringResource(R.string.worker_setting_desc), modifier = Modifier.padding(8.dp), fontSize = 13.sp, fontWeight = FontWeight.Light)
    }

}

@Composable
fun SpeedCell(num: Float, onChange: (Float) -> Unit = {}) {

    var value by remember(num) { mutableStateOf(num) }
    val interactionSource = remember { MutableInteractionSource() }
    val isDragging by interactionSource.collectIsDraggedAsState()
    val state = rememberSeekerState()
    val gap by animateDpAsState(if (isDragging) 2.dp else 0.dp)
    val thumbRadius by animateDpAsState(if (isDragging) 10.dp else 5.dp)

    fun getSizeName(size: Int): String {
        return when {
            size < 1000 -> "$size KB"
            else -> "~ ${size / 1024} MB"
        }
    }

    val text by remember(value) { mutableStateOf(getSizeName(value.toInt())) }

    val segments = listOf(
        Segment(name = "100Kb", start = 100f),
        Segment(name = "300Kb", start = 300f),
        Segment(name = "500Kb", start = 500f),
        Segment(name = "~1 Mb", start = 1024f),
        Segment(name = "~1.5 Mb", start = 1524f),
        Segment(name = "~2 Mb", start = 2048f),
        Segment(name = "~2.5 Mb", start = 2560f),
    )

    LaunchedEffect(isDragging) {
        if (!isDragging && num != value)
            onChange(value)
    }


    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.onSurface)
            .padding(10.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.speed_download_size), Modifier.weight(1f))
            Text(text = state.currentSegment.name, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(5.dp))

        Seeker(
            value = value,
            modifier = Modifier.height(20.dp),
            range = 100f..3072f,
            state = state,
            segments = segments,
            interactionSource = interactionSource,
            dimensions = SeekerDefaults.seekerDimensions(gap = gap, thumbRadius = thumbRadius),
            onValueChange = { value = it }
        )

        Text(text = stringResource(R.string.speed_download_seeting_desc), modifier = Modifier.padding(8.dp), fontSize = 13.sp, fontWeight = FontWeight.Light)
    }

}

@Composable
fun FrontingCell(address: String? = null, onChange: (String) -> Unit = {}) {

    var text by remember(address) { mutableStateOf(address ?: "") }

    LaunchedEffect(text) {
        if (address != text)
            onChange(text)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.onSurface)
            .padding(10.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.fronting_url), Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(5.dp))

        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
//                onChange(it)
            }, modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
            colors = TextFieldDefaults.outlinedTextFieldColors(unfocusedBorderColor = MaterialTheme.colors.primary.copy(0.4f))
        )

        Text(text = stringResource(R.string.fronting_setting_desc), modifier = Modifier.padding(8.dp), fontSize = 13.sp, fontWeight = FontWeight.Light)
    }

}