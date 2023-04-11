package ir.filternet.cfscanner.db.ref

import androidx.room.Embedded
import androidx.room.Relation
import ir.filternet.cfscanner.db.entity.*


data class ScanConfigISP(
    @Embedded val scanEntity: ScanEntity,
    @Relation(parentColumn = "configId", entityColumn = "uid")
    val configEntity: ConfigEntity,
    @Relation(parentColumn = "ispId", entityColumn = "uid")
    val ISPEntity: ISPEntity,
)
