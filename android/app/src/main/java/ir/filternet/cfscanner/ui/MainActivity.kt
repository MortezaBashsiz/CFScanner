package ir.filternet.cfscanner.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import dagger.hilt.android.AndroidEntryPoint
import ir.filternet.cfscanner.ui.navigation.CFScannerMainNavigation
import ir.filternet.cfscanner.ui.theme.CFScannerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CFScannerTheme {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colors.onBackground) {
                    CFScannerMainNavigation()
                }
            }
        }
    }
}
