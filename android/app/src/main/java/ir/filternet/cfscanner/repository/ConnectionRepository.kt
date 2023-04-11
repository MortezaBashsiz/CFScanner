package ir.filternet.cfscanner.repository

import ir.filternet.cfscanner.contracts.BasicRepository
import ir.filternet.cfscanner.mapper.mapToConnection
import ir.filternet.cfscanner.mapper.mapToConnectionEntity
import ir.filternet.cfscanner.model.Config
import ir.filternet.cfscanner.model.Connection
import ir.filternet.cfscanner.model.ISP
import ir.filternet.cfscanner.model.Scan
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionRepository @Inject constructor() : BasicRepository() {

    private val connectionDao by lazy { db.connectionDao() }

    suspend fun insert(connection: Connection) {
        connectionDao.insertAll(connection.mapToConnectionEntity())
    }

    suspend fun update(connection: Connection) {
        connectionDao.updateAll(connection.mapToConnectionEntity())
    }


    suspend fun insertAll(connections: List<Connection>) {
        connectionDao.insertAll(connections.map { it.mapToConnectionEntity() })
    }

    suspend fun getAllConnectionByScanID(scan: Scan): List<Connection> {
        return connectionDao.getConnectionsByScanID(scan.uid).map { it.mapToConnection().copy(scan = scan) }
    }

    suspend fun deleteByScanID(scan: Scan) {
        connectionDao.deleteAllByScanID(scan.uid)
    }


}