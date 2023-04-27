package ir.filternet.cfscanner.model

import java.util.*

data class Scan(
    val isp: ISP,
    val config: Config,
    val status: ScanResultStatus,
    val progress: ScanProgress = ScanProgress(),
    val scanCidrOrder:String = "" ,
    val creationDate: Date = Date(),
    val updateDate: Date = Date(),
    val uid: Int = 0,
)