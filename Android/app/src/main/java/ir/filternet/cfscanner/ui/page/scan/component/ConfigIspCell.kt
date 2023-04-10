package ir.filternet.cfscanner.ui.page.scan.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Webhook
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ir.filternet.cfscanner.model.Scan
import ir.filternet.cfscanner.ui.common.ScanningDetailsView
import ir.filternet.cfscanner.utils.parseToCommonName

@Composable
fun ConfigIspCell(scan: Scan) {
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 5.dp), horizontalArrangement = Arrangement.SpaceAround
    ) {
        ScanningDetailsView(Icons.Rounded.Language, scan.isp.parseToCommonName(context))
        ScanningDetailsView(Icons.Rounded.Webhook, scan.config.name)
    }
    Spacer(modifier = Modifier.height(20.dp))
}