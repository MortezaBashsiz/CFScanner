package ir.filternet.cfscanner.ui.destination

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ir.filternet.cfscanner.model.Scan
import ir.filternet.cfscanner.ui.page.sub.history.HistoryContract
import ir.filternet.cfscanner.ui.page.sub.history.HistoryScreen
import ir.filternet.cfscanner.ui.page.sub.history.HistoryScreenVM

@Composable
fun HistoryScreenDestination(
    navController: NavController,
    vm: HistoryScreenVM = hiltViewModel(),
    onScanSelect: (scan: Scan) -> Unit = {},
) {
    HistoryScreen(
        state = vm.viewState.value,
        effectFlow = vm.effect,
        onEventSent = { event -> vm.setEvent(event) },
        onNavigationRequested = { navigationEffect ->
            when (navigationEffect) {
                is HistoryContract.Effect.Navigation.ToScan -> {
                    onScanSelect(navigationEffect.scan)
                }
            }
        }
    )
}