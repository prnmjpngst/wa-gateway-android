package com.aji.wa_gateway.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aji.wa_gateway.db.entity.AppConfig

@Dao
interface AppConfigDao {
    @Query("SELECT * FROM app_config WHERE id = 1")
    suspend fun get(): AppConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: AppConfig)

    @Query("DELETE FROM app_config")
    suspend fun deleteAll()
}
