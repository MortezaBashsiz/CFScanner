package ir.filternet.cfscanner.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*

// Cloudflare CDN public ip entity from asn
@Entity(tableName = "cidrs",indices = [Index(value = ["address"],unique = true)])
data class CidrEntity(
    val address: String = "",
    val subnetMask: Int = 24,
    val date: Date = Date(),
    @PrimaryKey(autoGenerate = true) val uid: Int = 0,
)