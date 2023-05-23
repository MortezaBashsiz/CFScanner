package ir.filternet.cfscanner.ui.page.sub.scan_details.component

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CopyAll
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.filternet.cfscanner.R
import ir.filternet.cfscanner.model.Config
import ir.filternet.cfscanner.model.Connection
import ir.filternet.cfscanner.utils.applyNewAddress
import ir.filternet.cfscanner.utils.applyNewAddressByISPname
import ir.filternet.cfscanner.utils.convertToServerConfig
import ir.filternet.cfscanner.utils.parseToCommonName
import ir.filternet.cfscanner.utils.share2Clipboard

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConnectionCell(
    modifier: Modifier = Modifier,
    connection: Connection,
    speedChecking: Boolean = false,
    configs: List<Config> = emptyList(),
    update: () -> Unit = {},
) {

    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        )
    )

    val visibleSync = speedChecking == connection.updating
    val visibleAnim by animateFloatAsState(targetValue = if (visibleSync) 1f else 0f)


    Spacer(modifier = Modifier.height(4.dp))
    Card(
        modifier.fillMaxWidth(0.9f),
        backgroundColor = MaterialTheme.colors.onSurface
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(vertical = 8.dp, horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = connection.ip,
                Modifier.animateContentSize()
            )

            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {

                Row(Modifier.alpha(if (connection.delay <= 0) 0f else 1f)) {
                    Icon(Icons.Rounded.SignalCellularAlt, contentDescription = "Ping", Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = "${connection.delay}ms", fontSize = 11.sp)
                }


                Row(Modifier.alpha(if (connection.speed <= 0) 0f else 1f)) {
                    Icon(Icons.Rounded.Speed, contentDescription = "Speed", Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = "${connection.speed} kb/s", fontSize = 11.sp)

                }
            }

            Spacer(modifier = Modifier.width(10.dp))


            Icon(
                Icons.Rounded.Sync, contentDescription = "Sync",
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .rotate(if (connection.updating) angle else 0f)
                    .alpha(visibleAnim)
                    .clickable(!speedChecking && !connection.updating) { update() },
                tint = if (connection.updating) MaterialTheme.colors.primary else LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
            )


            Spacer(modifier = Modifier.width(10.dp))

            Box(contentAlignment = Alignment.Center) {
                var expanded by remember { mutableStateOf(false) }

                Icon(
                    Icons.Rounded.CopyAll, contentDescription = "Copy", modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .combinedClickable(onClick = {
                            val configID = connection.scan?.config?.uid
                            val scanIsp = connection.scan?.isp
                            val config = configs
                                .find { it.uid == configID }
                                ?.convertToServerConfig()
                                ?.applyNewAddressByISPname(connection.ip,scanIsp?.parseToCommonName(context))
                            if (config != null) {
                                Toast
                                    .makeText(context, context.getString(R.string.config_copied_to_clipboard), Toast.LENGTH_SHORT)
                                    .show()
                                share2Clipboard(context, config)
                                return@combinedClickable
                            } else {
                                Toast
                                    .makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT)
                                    .show()
                                clipboard.setText(AnnotatedString(connection.ip))
                                return@combinedClickable
                            }
                        }, onLongClick = {
                            if (configs.size > 1)
                                expanded = true
                        })
                )

                ConfigDropDown(
                    expanded, configs,
                    onSelect = {
                        val ispName = connection.scan?.isp
                        val config = it.convertToServerConfig().applyNewAddressByISPname(connection.ip,ispName?.parseToCommonName(context))?.let {
                            share2Clipboard(context, it)
                            Toast.makeText(context, context.getString(R.string.config_copied_to_clipboard), Toast.LENGTH_SHORT).show()
                        }
                    }, onDismiss = {
                        expanded = false
                    }
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

    }
    Spacer(modifier = Modifier.height(4.dp))
}