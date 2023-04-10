package ir.filternet.cfscanner.model

import androidx.room.PrimaryKey
import java.util.*

data class Scan(
    val isp: ISP,
    val config: Config,
    val status:ScanResultStatus,
    val progress:ScanProgress = ScanProgress(),
    val creationDate: Date = Date(),
    val updateDate: Date = Date(),
    @PrimaryKey(autoGenerate = true) val uid: Int = 0,
)