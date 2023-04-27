package ir.filternet.cfscanner.mapper

import ir.filternet.cfscanner.db.entity.ConnectionEntity
import ir.filternet.cfscanner.db.ref.ConnectionScan
import ir.filternet.cfscanner.model.Connection


fun ConnectionScan.mapToConnection() =
    Connection(
        connectionEntity.ip,
        null,
        cidrEntity.mapToCidr(),
        connectionEntity.speed,
        connectionEntity.delay,
        connectionEntity.create,
        connectionEntity.update,
        connectionEntity.uid
    )

fun ConnectionEntity.mapToConnection() =
    Connection(
        ip,
        null,
        null,
        speed,
        delay,
        create,
        update,
        uid
    )


fun Connection.mapToConnectionEntity() =
    ConnectionEntity(
        this.ip,
        scan!!.uid,
        cidr!!.uid,
        this.speed,
        this.delay,
        this.create,
        this.update,
        uid = this.id
    )