package com.aji.wa_gateway.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.aji.wa_gateway.db.entity.SendHistory

@Dao
interface SendHistoryDao {
    @Query("SELECT * FROM send_history ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int, offset: Int): List<SendHistory>

    @Query("SELECT * FROM send_history WHERE status = :status ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getPageByStatus(status: String, limit: Int, offset: Int): List<SendHistory>

    @Query("SELECT COUNT(*) FROM send_history")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM send_history WHERE status = :status")
    suspend fun countByStatus(status: String): Int

    @Query("SELECT * FROM send_history ORDER BY timestamp DESC LIMIT 5")
    suspend fun getRecent(): List<SendHistory>

    @Insert
    suspend fun insert(record: SendHistory)

    @Query("DELETE FROM send_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM send_history")
    suspend fun deleteAll()
}
