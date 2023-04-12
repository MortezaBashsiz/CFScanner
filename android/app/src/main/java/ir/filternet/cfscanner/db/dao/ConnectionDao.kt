package ir.filternet.cfscanner.db.dao

import androidx.room.*
import ir.filternet.cfscanner.db.ref.ScanConfigISP
import ir.filternet.cfscanner.db.entity.ConnectionEntity
import ir.filternet.cfscanner.db.ref.ConnectionScan

@Dao
interface ConnectionDao {

//    @Transaction
//    @Query("SELECT * FROM connections WHERE uid = :id LIMIT 1")
//    suspend fun getConnectionsByScanID(id:Int): ConnectionScan?
//
//    @Transaction
//    @Query("SELECT * FROM connections WHERE connections.configId = :configId AND connections.ispId = :ispID")
//    suspend fun getAllConnections(configId:Int,ispID:Int): List<ConnectionScan>

    @Transaction
    @Query("SELECT * FROM connections WHERE scanId = :id")
    suspend fun getConnectionsByScanID(id: Int): List<ConnectionScan>

    @Update
    suspend fun updateConnection(vararg connectionEntities: ConnectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg connectionEntities: ConnectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(connectionEntity: ConnectionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(connectionEntity: List<ConnectionEntity>): List<Long>

    @Update
    suspend fun updateAll(vararg connectionEntities: ConnectionEntity)

    @Delete
    suspend fun delete(connectionEntity: ConnectionEntity)

    @Query("DELETE FROM connections")
    suspend fun deleteAll()

    @Query("DELETE FROM connections WHERE scanId = :scanID")
    suspend fun deleteAllByScanID(scanID:Int)
}