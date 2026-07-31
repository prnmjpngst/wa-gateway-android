package com.aji.wa_gateway.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aji.wa_gateway.db.entity.Target

@Dao
interface TargetDao {
    @Query("SELECT * FROM targets ORDER BY fetchTimestamp DESC")
    suspend fun getAll(): List<Target>

    @Query("SELECT COUNT(*) FROM targets")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(targets: List<Target>)

    @Query("DELETE FROM targets")
    suspend fun deleteAll()
}
