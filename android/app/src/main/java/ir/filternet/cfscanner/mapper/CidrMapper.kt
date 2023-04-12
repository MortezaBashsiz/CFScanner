package ir.filternet.cfscanner.mapper

import ir.filternet.cfscanner.db.entity.CidrEntity
import ir.filternet.cfscanner.model.CIDR

fun CidrEntity.mapToCidr(): CIDR = CIDR(address, subnetMask, date, uid)