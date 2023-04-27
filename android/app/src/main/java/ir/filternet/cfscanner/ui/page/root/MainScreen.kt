package ir.filternet.cfscanner.ui.page.root

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import ir.filternet.cfscanner.R
import ir.filternet.cfscanner.contracts.SIDE_EFFECTS_KEY
import ir.filternet.cfscanner.model.Update
import ir.filternet.cfscanner.model.UpdateState
import ir.filternet.cfscanner.service.CloudScannerService
import ir.filternet.cfscanner.service.CloudUpdateService
import ir.filternet.cfscanner.ui.navigation.CFScannerSubNavigation
import ir.filternet.cfscanner.ui.page.root.components.PlayfulBottomNavigation
import ir.filternet.cfscanner.ui.page.root.components.UpdateHeader
import ir.filternet.cfscanner.utils.installFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@Composable
fun MainScreen(
    state: MainContract.State,
    effectFlow: Flow<MainContract.Effect>?,
    onEventSent: (event: MainContract.Event) -> Unit,
    onNavigationRequested: (navigationEffect: MainContract.Effect.Navigation) -> Unit,
) {

    var update by remember { mutableStateOf(null as Update?) }
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

                is MainContract.Effect.UpdateAvailable -> {
                    update = effect.update
                }
            }
        }?.collect()
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Column {
            UpdateHeader(update,
                onDownload = {
                    ContextCompat.startForegroundService(context, Intent(context, CloudUpdateService::class.java))
                    onEventSent.invoke(MainContract.Event.StartDownloadUpdate)
                },
                onCancel = {
                    onEventSent.invoke(MainContract.Event.StopDownloadUpdate)
                },
                onInstall = {
                    if(update?.state is UpdateState.Downloaded){
                        (update?.state as UpdateState.Downloaded).file.let {
                            context.installFile(it)
                        }
                    }
                },
                onDismiss = {
                    update = null
                }
            )

            CFScannerSubNavigation(selectedIndex, { scan ->
                onNavigationRequested.invoke(MainContract.Effect.Navigation.ToScan(scan))
            }, {
                onNavigationRequested.invoke(MainContract.Effect.Navigation.ToCidrManagement)
            }) {
                onEventSent.invoke(MainContract.Event.SelectTabIndex(it))
            }
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
