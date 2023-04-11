package ir.filternet.cfscanner.ui.page.sub.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.filternet.cfscanner.R
import ir.filternet.cfscanner.ui.page.sub.history.component.ScanCardItem
import ir.filternet.cfscanner.ui.page.sub.scan.component.LoadingView
import kotlinx.coroutines.flow.Flow

@Composable
fun HistoryScreen(
    state: HistoryContract.State,
    effectFlow: Flow<HistoryContract.Effect>?,
    onEventSent: (event: HistoryContract.Event) -> Unit,
    onNavigationRequested: (navigationEffect: HistoryContract.Effect.Navigation) -> Unit,
) {
    val scans = state.scans
    val loading = state.loading

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (!loading)
            if (scans.isNotEmpty()) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(top = 20.dp , bottom = 80.dp)
                ) {
                    item {
                        Text(text = stringResource(R.string.scan_history), fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                    items(scans) {
                        Spacer(modifier = Modifier.height(5.dp))
                        ScanCardItem(it) {
                            onNavigationRequested.invoke(HistoryContract.Effect.Navigation.ToScan(it))
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                    }
                }
            } else {
                Text(text = stringResource(R.string.history_empty))
            }
        else
            LoadingView()
    }
}

