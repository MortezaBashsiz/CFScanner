package ir.filternet.cfscanner.ui.page.sub.scan_details.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ir.filternet.cfscanner.R
import ir.filternet.cfscanner.ui.theme.Gray
import ir.filternet.cfscanner.ui.theme.Green
import ir.filternet.cfscanner.ui.theme.Red

@Composable
fun ActionCell(disableResume: Boolean = false, disableDelete: Boolean = false, resume: () -> Unit = {}, stop: () -> Unit = {}, delete: () -> Unit = {}) {
    Spacer(modifier = Modifier.height(20.dp))
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Row(Modifier.fillMaxWidth(0.8f)) {

            val buttonText = when {
                disableResume && disableDelete -> stringResource(R.string.stop_scan)
                else -> stringResource(R.string.resume_scan)
            }

            val enabled = when {
                disableResume && disableDelete -> true
                !disableResume -> true
                else -> false
            }

            val color = when {
                disableResume && disableDelete -> Red
                !disableResume -> Green
                else -> Gray
            }

            Card(
                Modifier
                    .weight(1f)
                    .height(45.dp),
                backgroundColor = color
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(enabled) {
                            if (disableResume && disableDelete) {
                                stop()
                            } else {
                                resume()
                            }

                        }, contentAlignment = Alignment.Center
                ) {
                    Text(text = buttonText)
                }
            }


            Spacer(modifier = Modifier.width(10.dp))

            Card(
                Modifier.size(45.dp),
                backgroundColor = if (disableDelete) Gray else Red
            ) {
                Box(
                    modifier = Modifier.clickable(!disableDelete) { delete() }, contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete")
                }
            }
        }

    }
    Spacer(modifier = Modifier.height(20.dp))
}