package ir.filternet.cfscanner.model

import java.util.*

data class Connection(
    val ip : String = "",
    val scan:Scan?,
    val cidr: CIDR?,
    val speed:Long = -1,
    val delay:Long = -1,
    val update: Date = Date(),
    val create: Date = Date(),
    val id:Int = 0,
    val updating:Boolean = false
)