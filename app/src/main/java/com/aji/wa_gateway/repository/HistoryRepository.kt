package com.aji.wa_gateway.repository

import com.aji.wa_gateway.db.dao.SendHistoryDao
import com.aji.wa_gateway.db.entity.SendHistory

class HistoryRepository(private val sendHistoryDao: SendHistoryDao) {
    suspend fun getPage(page: Int, pageSize: Int = 20): List<SendHistory> {
        val offset = (page - 1) * pageSize
        return sendHistoryDao.getPage(pageSize, offset)
    }

    suspend fun getPageByStatus(status: String, page: Int, pageSize: Int = 20): List<SendHistory> {
        val offset = (page - 1) * pageSize
        return sendHistoryDao.getPageByStatus(status, pageSize, offset)
    }

    suspend fun count(): Int = sendHistoryDao.count()

    suspend fun countByStatus(status: String): Int = sendHistoryDao.countByStatus(status)

    suspend fun getRecent(): List<SendHistory> = sendHistoryDao.getRecent()

    suspend fun insert(record: SendHistory) = sendHistoryDao.insert(record)

    suspend fun deleteById(id: Long) = sendHistoryDao.deleteById(id)

    suspend fun clearAll() = sendHistoryDao.deleteAll()
}
