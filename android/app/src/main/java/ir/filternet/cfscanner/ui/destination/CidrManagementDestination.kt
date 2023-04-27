package ir.filternet.cfscanner.ui.destination

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ir.filternet.cfscanner.ui.page.sub.cidr_management.CidrManagementContract
import ir.filternet.cfscanner.ui.page.sub.cidr_management.CidrManagementScreen
import ir.filternet.cfscanner.ui.page.sub.cidr_management.CidrManagementScreenVM


@Composable
fun CidrManagementDestination(
    navController: NavController,
    vm: CidrManagementScreenVM = hiltViewModel()
) {
    CidrManagementScreen(
        state = vm.viewState.value,
        effectFlow = vm.effect,
        onEventSent = { event ->  vm.setEvent(event) },
        onNavigationRequested = { navigationEffect ->
            when(navigationEffect) {
                is CidrManagementContract.Effect.Navigation.NavigateUP -> navController.navigateUp()
            }
        }
    )
}