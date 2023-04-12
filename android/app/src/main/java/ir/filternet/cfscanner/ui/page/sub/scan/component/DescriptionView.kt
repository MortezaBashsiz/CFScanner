package ir.filternet.cfscanner.ui.page.sub.scan.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ir.filternet.cfscanner.R
import ir.filternet.cfscanner.ui.theme.Gray

@Composable
fun ColumnScope.DescriptionView() {
    Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 70.dp), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.no_connection_so_far),
            style = LocalTextStyle.current.copy(color = Gray.copy(0.4f))
        )
    }
}