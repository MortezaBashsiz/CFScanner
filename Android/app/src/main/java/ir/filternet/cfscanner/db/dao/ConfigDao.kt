package ir.filternet.cfscanner.db.dao

import androidx.room.*
import ir.filternet.cfscanner.db.entity.ConfigEntity

@Dao
interface ConfigDao {
    @Query("SELECT * FROM configs")
    suspend fun getAllConfigs(): List<ConfigEntity>

    @Query("SELECT * FROM configs WHERE uid IN (:configIds)")
    suspend fun loadAllByIds(configIds: IntArray): List<ConfigEntity>

    @Query("SELECT * FROM configs WHERE uid = :id LIMIT 1")
    suspend fun findById(id: Int): ConfigEntity

    @Update
    suspend fun updateConfig(vararg configEntity: ConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg configEntities: ConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(configEntity: ConfigEntity):Long

    @Delete
    suspend fun delete(configEntity: ConfigEntity)
}