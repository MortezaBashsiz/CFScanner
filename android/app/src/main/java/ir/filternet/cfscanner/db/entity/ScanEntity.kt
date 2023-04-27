package ir.filternet.cfscanner.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import ir.filternet.cfscanner.model.ScanProgress
import ir.filternet.cfscanner.model.ScanResultStatus
import java.util.*

@Entity(tableName = "scans", foreignKeys = [
    ForeignKey(
        entity = ConfigEntity::class,
        parentColumns = ["uid"],
        childColumns = ["configId"],
        onDelete = ForeignKey.CASCADE,
    ),
])
data class ScanEntity(
    val ispId: Int,
    val configId: Int,
    val status:ScanResultStatus,
    val progress:ScanProgress,
    val scanCidrOrder:String = "" ,
    val creationDate: Date = Date(),
    val updateDate: Date = Date(),
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(index = true)
    val uid: Int = 0,
)