package ir.filternet.cfscanner.db.entity

import androidx.room.*
import java.util.*

@Entity(tableName = "connections")
data class ConnectionEntity(
    val ip : String = "",
    val scanId: Int,
    val cidrId: Int,
    val speed:Long = -1,
    val delay:Long = -1,
    val create: Date = Date(),
    val update: Date = Date(),
    @PrimaryKey(autoGenerate = true) val uid: Int = 0,
)
