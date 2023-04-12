package ir.filternet.cfscanner.ui.destination

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ir.filternet.cfscanner.service.CloudScannerService
import ir.filternet.cfscanner.service.CloudSpeedService
import ir.filternet.cfscanner.ui.page.scan.ScanDetailsContract
import ir.filternet.cfscanner.ui.page.scan.ScanDetailsScreen
import ir.filternet.cfscanner.ui.page.scan.ScanDetailsScreenVM
import ir.filternet.cfscanner.utils.BindService

@Composable
fun ScanDetailsScreenDestination(
    scanId: Int,
    navController: NavController,
    vm: ScanDetailsScreenVM = hiltViewModel<ScanDetailsScreenVM>(),
) {

    // bind service to this page
    BindService(CloudScannerService::class.java,vm)
    BindService(CloudSpeedService::class.java,vm)


    // set scan id just once to ViewModel
    LaunchedEffect(vm) {
        vm.setScan(scanId)
    }

    ScanDetailsScreen(
        state = vm.viewState.value,
        effectFlow = vm.effect,
        onEventSent = { event -> vm.setEvent(event) },
        onNavigationRequested = { navigationEffect ->
            when(navigationEffect){
                ScanDetailsContract.Effect.Navigation.ToUp -> {
                    navController.navigateUp()
                }
            }
        }
    )
}