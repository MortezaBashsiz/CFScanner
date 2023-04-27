package ir.filternet.cfscanner.db.ref

import androidx.room.Embedded
import androidx.room.Relation
import ir.filternet.cfscanner.db.entity.*


data class ConnectionScan(
    @Embedded val connectionEntity: ConnectionEntity,
//    @Relation(parentColumn = "scanId", entityColumn = "uid")
//    val scanEntity: ScanConfigISP,
    @Relation(parentColumn = "cidrId", entityColumn = "uid")
    val cidrEntity: CidrEntity,
)
