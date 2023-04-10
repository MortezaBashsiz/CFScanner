package ir.filternet.cfscanner.model

import java.util.*


data class Config(
    val config: String = "",
    val name:String = "",
    val date: Date = Date(),
    val uid: Int = 0,
    val selected:Boolean = false,
    val active:Boolean = false,
)