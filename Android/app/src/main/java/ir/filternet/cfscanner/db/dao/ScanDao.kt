package ir.filternet.cfscanner.db.dao

import androidx.room.*
import ir.filternet.cfscanner.db.entity.CidrEntity
import ir.filternet.cfscanner.db.entity.ScanEntity
import ir.filternet.cfscanner.db.ref.ScanConfigISP

@Dao
interface ScanDao {

    @Transaction
    @Query("SELECT * FROM scans WHERE uid = :id LIMIT 1")
    suspend fun getScansByID(id:Int): ScanConfigISP?

    @Transaction
    @Query("SELECT * FROM scans WHERE scans.configId = :configId")
    suspend fun getAllScans(configId:Int): List<ScanConfigISP>

    @Transaction
    @Query("SELECT * FROM scans WHERE scans.configId = :configId AND scans.ispId = :ispID")
    suspend fun getAllScans(configId:Int,ispID:Int): List<ScanConfigISP>

    @Transaction
    @Query("SELECT * FROM scans")
    suspend fun getAllScans(): List<ScanConfigISP>

    @Query("SELECT * FROM scans")
    suspend fun getAll(): List<ScanEntity>

    @Query("SELECT * FROM scans WHERE uid = :id LIMIT 1")
    suspend fun findById(id: Int): ScanEntity?

    @Update
    suspend fun updateScan(vararg scans: ScanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg scans: ScanEntity):List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(scan: ScanEntity):Long

    @Delete
    suspend fun delete(scan: ScanEntity)

    @Query("DELETE FROM scans")
    suspend fun deleteAll()
}