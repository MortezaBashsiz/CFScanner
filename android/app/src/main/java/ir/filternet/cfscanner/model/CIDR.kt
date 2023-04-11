package ir.filternet.cfscanner.model

import java.util.*

data class CIDR(
    val address: String = "",
    val subnetMask: Int = 24,
    val date: Date = Date(),
    val uid: Int = 0,
    val opened:Boolean = false
)
