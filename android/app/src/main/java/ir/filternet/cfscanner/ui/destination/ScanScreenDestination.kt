package ir.filternet.cfscanner.ui.destination

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ir.filternet.cfscanner.service.CloudScannerService
import ir.filternet.cfscanner.ui.page.sub.scan.ScanScreen
import ir.filternet.cfscanner.ui.page.sub.scan.ScanScreenVM
import ir.filternet.cfscanner.utils.BindService


@Composable
fun ScanScreenDestination(
    navController: NavController,
    vm: ScanScreenVM = hiltViewModel()
) {
    BindService(CloudScannerService::class.java,vm)
    ScanScreen(
        state = vm.viewState.value,
        effectFlow = vm.effect,
        onEventSent = { event ->  vm.setEvent(event) },
        onNavigationRequested = { navigationEffect ->

        }
    )
}