package ir.filternet.cfscanner.mapper

import ir.filternet.cfscanner.db.entity.ConfigEntity
import ir.filternet.cfscanner.model.Config

fun ConfigEntity.mapToConfig():Config = Config(config,name,date,uid)
fun Config.mapToConfigEntity():ConfigEntity = ConfigEntity(config,name,date,uid)