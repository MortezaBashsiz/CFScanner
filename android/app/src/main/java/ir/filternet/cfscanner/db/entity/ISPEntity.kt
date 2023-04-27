package ir.filternet.cfscanner.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*

// hold all isp while FCScanner service was running
@Entity(tableName = "isps",indices = [Index(value = ["name"],unique = true)])
data class ISPEntity (
    val name: String = "",
    val region:String?="",
    val city:String?="",
    val tower:String? = "",
    val date:Date = Date(),
    @PrimaryKey(autoGenerate = true) val uid: Int = 0,
)