package ir.filternet.cfscanner.model

import java.util.*

data class CIDR(
    val address: String = "",
    val subnetMask: Int = 24,
    val date: Date = Date(),
    val uid: Int = 0,
    val custom:Boolean = false,
    val position:Int = -1,
    val opened:Boolean = false
)
