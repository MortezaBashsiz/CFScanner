package ir.filternet.cfscanner.db.dao

import androidx.room.*
import ir.filternet.cfscanner.db.entity.ISPEntity

@Dao
interface ISPDao {
    @Query("SELECT * FROM isps")
    suspend fun getAllISP(): List<ISPEntity>

    @Query("SELECT * FROM isps WHERE uid IN (:ids)")
    suspend fun loadAllByIds(ids: IntArray): List<ISPEntity>

    @Query("SELECT * FROM isps WHERE uid = :id LIMIT 1")
    suspend fun findIspById(id: Int): ISPEntity?

    @Query("SELECT * FROM isps WHERE name LIKE :name LIMIT 1")
    suspend fun findIspByName(name: String): ISPEntity?

    @Update
    suspend fun updateIsp(vararg isp: ISPEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg isps: ISPEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(isp: ISPEntity):Long

    @Delete
    suspend fun delete(isp: ISPEntity)
}