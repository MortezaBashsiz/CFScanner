package ir.filternet.cfscanner.model

import ir.filternet.cfscanner.scanner.v2ray.V2rayConfig
import java.util.*


data class Config(
    val config: String = "",
    val name:String = "",
    val date: Date = Date(),
    val uid: Int = 0,
    val selected:Boolean = false,
    val active:Boolean = false,
    val v2rayConfig: V2rayConfig?= null,
)