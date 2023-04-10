package ir.filternet.cfscanner.mapper

import ir.filternet.cfscanner.db.entity.ScanEntity
import ir.filternet.cfscanner.db.ref.ScanConfigISP
import ir.filternet.cfscanner.model.Scan


fun ScanConfigISP.mapToScan() =
    Scan(
        ISPEntity.mapToISP(),
        configEntity.mapToConfig(),
        this.scanEntity.status,
        this.scanEntity.progress,
        this.scanEntity.creationDate,
        this.scanEntity.updateDate,
        this.scanEntity.uid
    )


fun Scan.mapToScanEntity() =
    ScanEntity(
        isp.uid,
        config.uid,
        status,
        progress,
        creationDate,
        updateDate,
        uid
    )