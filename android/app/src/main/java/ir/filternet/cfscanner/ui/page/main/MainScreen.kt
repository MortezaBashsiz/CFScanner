package ir.filternet.cfscanner.ui.page.main

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import ir.filternet.cfscanner.R
import ir.filternet.cfscanner.contracts.SIDE_EFFECTS_KEY
import ir.filternet.cfscanner.ui.navigation.CFScannerSubNavigation
import ir.filternet.cfscanner.ui.page.main.components.PlayfulBottomNavigation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

@Composable
fun MainScreen(
    state: MainContract.State,
    effectFlow: Flow<MainContract.Effect>?,
    onEventSent: (event: MainContract.Event) -> Unit,
    onNavigationRequested: (navigationEffect: MainContract.Effect.Navigation) -> Unit,
) {

    val selectedIndex = state.selectedIndex

    val context = LocalContext.current
    LaunchedEffect(SIDE_EFFECTS_KEY) {
        effectFlow?.onEach { effect ->
            when (effect) {
                is MainContract.Effect.Messenger -> {
                    when (effect) {
                        is MainContract.Effect.Messenger.Toast -> {
                            val message = effect.message ?: context.getString(effect.messageId!!)
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
                is MainContract.Effect.Navigation -> {
                    onNavigationRequested(effect)
                }
            }
        }?.collect()
    }

    Box(modifier = Modifier.fillMaxSize()) {

        CFScannerSubNavigation(selectedIndex, { scan ->
            onNavigationRequested.invoke(MainContract.Effect.Navigation.ToScan(scan))
        }) {
            onEventSent.invoke(MainContract.Event.SelectTabIndex(it))
        }

        PlayfulBottomNavigation(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            index = selectedIndex,
            icons = arrayOf(R.drawable.ic_cloud_done, R.drawable.ic_network_check, R.drawable.ic_settings),
            unselectedColor = MaterialTheme.colors.onPrimary.copy(0.8f),
            selectedColor = MaterialTheme.colors.onPrimary,
            indicatorColor = MaterialTheme.colors.onPrimary,
            backgroundColor = MaterialTheme.colors.primary
        ) {
            onEventSent.invoke(MainContract.Event.SelectTabIndex(it))
        }

    }
}
