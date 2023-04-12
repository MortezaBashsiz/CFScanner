package ir.filternet.cfscanner.db.dao

import androidx.room.*
import ir.filternet.cfscanner.db.entity.CidrEntity

@Dao
interface CIDRDao {
    @Query("SELECT * FROM cidrs")
    suspend fun getAll(): List<CidrEntity>

    @Query("SELECT * FROM cidrs WHERE uid = :id LIMIT 1")
    suspend fun findById(id: Int): CidrEntity?

    @Query("SELECT * FROM cidrs WHERE address = :address LIMIT 1")
    suspend fun findByAddress(address: String): CidrEntity?

    @Update
    suspend fun updateConfig(vararg cidrEntities: CidrEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg cidrEntities: CidrEntity):List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(cidrEntity: CidrEntity):Long

    @Delete
    suspend fun delete(cidrEntity: CidrEntity)

    @Query("DELETE FROM cidrs")
    suspend fun deleteAll()
}