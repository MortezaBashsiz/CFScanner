package ir.filternet.cfscanner.repository

import ir.filternet.cfscanner.contracts.BasicRepository
import ir.filternet.cfscanner.mapper.mapToScan
import ir.filternet.cfscanner.mapper.mapToScanEntity
import ir.filternet.cfscanner.model.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanRepository @Inject constructor() : BasicRepository() {

    private val scanDao by lazy { db.scanDao() }

    suspend fun createScan(config: Config, ips: ISP): Scan {
        val scan = Scan(ips, config, ScanResultStatus.PAUSED)
        val scanID = scanDao.insert(scan.mapToScanEntity())
        return scan.copy(uid = scanID.toInt())
    }

    suspend fun updateScanProgress(scan: Scan) {
        scanDao.updateScan(scan.copy(updateDate = Date()).mapToScanEntity())
    }

    suspend fun getAllScans(): List<Scan> {
        return scanDao.getAllScans().map { it.mapToScan() }
    }

    suspend fun getScanById(id: Int): Scan? {
        return scanDao.getScansByID(id)?.mapToScan()
    }

    suspend fun deleteScan(scan: Scan) {
        return scanDao.delete(scan.mapToScanEntity())
    }

}