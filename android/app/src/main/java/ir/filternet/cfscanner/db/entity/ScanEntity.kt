package ir.filternet.cfscanner.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ir.filternet.cfscanner.model.ScanProgress
import ir.filternet.cfscanner.model.ScanResultStatus
import java.util.*

@Entity(tableName = "scans")
data class ScanEntity(
    val ispId: Int,
    val configId: Int,
    val status:ScanResultStatus,
    val progress:ScanProgress,
    val creationDate: Date = Date(),
    val updateDate: Date = Date(),
    @PrimaryKey(autoGenerate = true) val uid: Int = 0,
)