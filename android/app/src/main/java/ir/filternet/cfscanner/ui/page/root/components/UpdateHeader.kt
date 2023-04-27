package ir.filternet.cfscanner.ui.page.root.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.filternet.cfscanner.R
import ir.filternet.cfscanner.model.Update
import ir.filternet.cfscanner.model.UpdateState
import ir.filternet.cfscanner.ui.theme.Green
import ir.filternet.cfscanner.utils.clickableWithNoRipple
import ir.filternet.cfscanner.utils.round

@Composable
fun UpdateHeader(
    update: Update?,
    onDownload: () -> Unit = {},
    onCancel: () -> Unit = {},
    onInstall: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    val height by animateDpAsState(targetValue = if (update != null) 40.dp else 0.dp)
    var showDialog by remember { mutableStateOf(false) }


    if (showDialog && update != null) {
        UpdateDialog(
            update,
            download = onDownload,
            install = onInstall,
            cancel = onCancel,
        ) {
            showDialog = false
        }
    }

    val text = when (update?.state) {
        is UpdateState.Downloaded -> stringResource(R.string.update_ready_to_install)
        is UpdateState.Downloading -> stringResource(R.string.update_downloading, ((update.state as UpdateState.Downloading).progress*100).toInt().toString())
        UpdateState.Idle -> stringResource(R.string.update_available)
        null -> ""
    }

    val color = when (update?.state) {
        is UpdateState.Downloaded -> Green
        else -> MaterialTheme.colors.primaryVariant
    }

    Box {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .background(color)
        )

        if (update?.state is UpdateState.Downloading) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth((update.state as UpdateState.Downloading).progress)
                    .height(height)
                    .background(MaterialTheme.colors.primary)
            )
        }


        Row(
            Modifier
                .fillMaxWidth()
                .height(height)
                .clickableWithNoRipple { showDialog = true }
                .animateContentSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (update != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Rounded.RocketLaunch, contentDescription = "Cancel", tint = MaterialTheme.colors.background)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = text, color = MaterialTheme.colors.background, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Cancel",
                    Modifier
                        .size(30.dp)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colors.background.copy(0.1f))
                        .clickable { onDismiss() }
                        .padding(4.dp),
                    tint = MaterialTheme.colors.background
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}