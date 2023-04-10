package ir.filternet.cfscanner.ui.destination

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ir.filternet.cfscanner.ui.page.sub.settings.ScanSettingScreen
import ir.filternet.cfscanner.ui.page.sub.settings.ScanSettingsScreenVM

@Composable
fun ScanSettingScreenDestination(
    navController: NavController,
    vm: ScanSettingsScreenVM = hiltViewModel()
) {
    ScanSettingScreen(
        state = vm.viewState.value,
        effectFlow = vm.effect,
        onEventSent = { event ->  vm.setEvent(event) },
        onNavigationRequested = { navigationEffect ->

        }
    )
}
