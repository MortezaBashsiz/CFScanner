package ir.filternet.cfscanner.ui.page.sub.scan.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.filternet.cfscanner.R
import ir.filternet.cfscanner.model.Config
import ir.filternet.cfscanner.ui.theme.Red
import kotlin.random.Random

@Composable
fun ConfigEditDialog(
    config: Config,
    update: (Config) -> Unit = {},
    delete: (Config) -> Unit = {},
    dismiss: () -> Unit = {},
) {
    Dialog(onDismissRequest = { dismiss() }, properties = DialogProperties()) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 370.dp, min = 250.dp)
                .aspectRatio(1f),
            elevation = 5.dp,
            shape = RoundedCornerShape(7.dp),
            backgroundColor = MaterialTheme.colors.surface
        ) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {

                val focusRequester = remember { FocusRequester() }
                val textColor = MaterialTheme.colors.onBackground
                val interactionSource = remember { MutableInteractionSource() }
                val isFocused by interactionSource.collectIsFocusedAsState()
                var configName by remember { mutableStateOf(config.name.let { it.ifEmpty { "Config " + Random.nextInt(231,999) } }) }

                Spacer(modifier = Modifier.height(20.dp))

                BasicTextField(
                    value = configName.let { if (!isFocused) "$it ✏️" else it },
                    onValueChange = { configName = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onGloballyPositioned { focusRequester.requestFocus() },
                    singleLine = true,
                    maxLines = 1,
                    minLines = 1,
                    interactionSource = interactionSource,
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, color = textColor),
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = config.config,
                    color = textColor.copy(0.4f),
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colors.primary, RoundedCornerShape(7.dp))
                        .padding(4.dp)
                        .fillMaxWidth(0.9f),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(Modifier.fillMaxWidth(0.90f)) {
                    Card(
                        Modifier
                            .weight(1f)
                            .height(40.dp),
                        backgroundColor = MaterialTheme.colors.primary
                    ) {
                        Box(
                            modifier = Modifier
                                .clickable {
                                    update(config.copy(name = configName))
                                    dismiss()
                                }
                                .fillMaxSize(), contentAlignment = Alignment.Center
                        ) {
                            Text(text = stringResource(R.string.save))
                        }
                    }

                    if (config.uid > 0) {
                        Spacer(modifier = Modifier.width(10.dp))

                        Card(
                            Modifier.size(40.dp),
                            backgroundColor = Red
                        ) {
                            Box(
                                modifier = Modifier.clickable { delete(config); dismiss() }, contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colors.background)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                LaunchedEffect(Unit) {
                    if (configName.isEmpty()) {
                        focusRequester.requestFocus()
                    } else {
                        focusRequester.freeFocus()
                    }
                }
            }
        }
    }
}