package ir.filternet.cfscanner.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ir.filternet.cfscanner.model.Scan
import ir.filternet.cfscanner.ui.destination.HistoryScreenDestination
import ir.filternet.cfscanner.ui.destination.ScanScreenDestination
import ir.filternet.cfscanner.ui.destination.ScanSettingScreenDestination

@Composable
fun CFScannerSubNavigation(index: Int, parentNavigator: (scan:Scan) -> Unit = {}, callback: (Int) -> Unit = {}) {
    val navController = rememberNavController()

    val currentRoute by remember(index) {
        mutableStateOf(
            when (index) {
                0 -> Navigation.SubRoutes.HISTORY
                1 -> Navigation.SubRoutes.SCAN
                else -> Navigation.SubRoutes.SETTINGS
            }
        )
    }

    BackHandler(currentRoute != Navigation.SubRoutes.SCAN) {
        callback(1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        verticalArrangement = Arrangement.Center,
    ) {
        NavHost(
            navController = navController,
            startDestination = Navigation.SubRoutes.SCAN,
        ) {

            composable(Navigation.SubRoutes.SCAN) {
                ScanScreenDestination(navController = navController)
            }

            composable(Navigation.SubRoutes.HISTORY) {
                HistoryScreenDestination(navController = navController){
                    parentNavigator(it)
                }
            }

            composable(Navigation.SubRoutes.SETTINGS) {
                ScanSettingScreenDestination(navController = navController)
            }
        }
    }

    LaunchedEffect(currentRoute) {
        navController.navigate(currentRoute) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = currentRoute != Navigation.SubRoutes.SCAN
            }
            launchSingleTop = true
            restoreState = currentRoute == Navigation.SubRoutes.SCAN
        }
    }
}