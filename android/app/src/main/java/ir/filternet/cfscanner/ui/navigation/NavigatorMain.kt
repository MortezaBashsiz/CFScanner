package ir.filternet.cfscanner.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavDeepLink
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import ir.filternet.cfscanner.ui.destination.MainScreenDestination
import ir.filternet.cfscanner.ui.destination.ScanDetailsScreenDestination
import ir.filternet.cfscanner.ui.navigation.Navigation.MainRoutes.ScanDetailsArgs.SCAN_ID

@Composable
fun CFScannerMainNavigation() {
    val navController = rememberNavController()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        verticalArrangement = Arrangement.Center
    ) {
        NavHost(
            navController = navController,
            startDestination = Navigation.MainRoutes.MAIN,
        ) {

            composable(Navigation.MainRoutes.MAIN) {
                MainScreenDestination(navController)
            }

            composable(
                Navigation.MainRoutes.SCAN_DETAILS,
                arguments = listOf(navArgument(name = SCAN_ID) { type = NavType.IntType }),
                deepLinks = listOf(navDeepLink{uriPattern = "cfscanner://scandetails/?id={$SCAN_ID}"})
            ) {
                val scanId = requireNotNull(it.arguments?.getInt(SCAN_ID)) { "Scan id is required as an argument" }
                ScanDetailsScreenDestination(scanId, navController)
            }

            composable(Navigation.MainRoutes.VPN_SETTINGS) {

            }

            composable(Navigation.MainRoutes.SCAN_SETTINGS) {

            }

        }
    }
}