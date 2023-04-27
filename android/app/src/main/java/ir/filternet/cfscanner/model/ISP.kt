package ir.filternet.cfscanner.model

import java.util.*

data class ISP(
    val name: String = "",
    val region:String?="",
    val city:String?="",
    val tower:String? = "",
    val date: Date = Date(),
    val uid: Int = 0,
    val selected:Boolean = false,
    val active:Boolean = false,
)