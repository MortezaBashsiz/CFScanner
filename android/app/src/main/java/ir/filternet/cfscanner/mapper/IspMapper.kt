package ir.filternet.cfscanner.mapper

import ir.filternet.cfscanner.db.entity.ISPEntity
import ir.filternet.cfscanner.model.ISP

fun ISPEntity.mapToISP(): ISP = ISP(name, region,city,tower, date, uid)


fun ISP.mapToIspEntity() = ISPEntity(name, region,city,tower, date, uid)