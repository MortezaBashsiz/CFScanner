package ir.filternet.cfscanner.ui.navigation

import ir.filternet.cfscanner.ui.navigation.Navigation.MainRoutes.ScanDetailsArgs.SCAN_ID

object Navigation {

    object MainRoutes {
        const val MAIN = "main"
        const val SCAN_SETTINGS = "scan_settings"
        const val VPN_SETTINGS = "vpn_settings"

        object ScanDetailsArgs {
            const val SCAN_ID = "scan_id"
        }
        const val SCAN_DETAILS = "scan_details/{$SCAN_ID}"


        fun ScanDetailsRoute(scanID:Int):String{
            return "scan_details/$scanID"
        }
    }

    object SubRoutes {
        const val SCAN = "scan"
        const val HISTORY = "history"
        const val SETTINGS = "settings"
//        const val VPN = "vpn"
    }

}