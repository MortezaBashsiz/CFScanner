package ir.filternet.cfscanner.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "configs")
data class ConfigEntity(
    val config: String = "",
    val name: String = "",
    val date: Date = Date(),
    @PrimaryKey(autoGenerate = true) val uid: Int = 0,
)