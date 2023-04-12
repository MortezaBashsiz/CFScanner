package ir.filternet.cfscanner.ui.page.scan.component

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
fun ActionCell(disableResume:Boolean = false , disableDelete:Boolean = false ,resume:()->Unit = {} ,delete: () -> Unit = {}) {
    Spacer(modifier = Modifier.height(20.dp))
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Row(Modifier.fillMaxWidth(0.8f)) {
            Card(
                Modifier
                    .weight(1f)
                    .height(45.dp),
                backgroundColor = if(disableResume) Gray else Green
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(!disableResume) { resume() }, contentAlignment = Alignment.Center
                ) {
                    Text(text = stringResource(R.string.resume))
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Card(
                Modifier.size(45.dp),
                backgroundColor = if(disableDelete) Gray else Red
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