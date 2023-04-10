package ir.filternet.cfscanner.ui.page.sub.scan.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoadingView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.7f),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(Modifier.size(50.dp))
    }
}