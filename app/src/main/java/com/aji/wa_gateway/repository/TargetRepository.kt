package com.aji.wa_gateway.repository

import com.aji.wa_gateway.db.dao.TargetDao
import com.aji.wa_gateway.db.entity.Target

class TargetRepository(private val targetDao: TargetDao) {
    suspend fun getAll(): List<Target> = targetDao.getAll()

    suspend fun count(): Int = targetDao.count()

    suspend fun insertAll(targets: List<Target>) = targetDao.insertAll(targets)

    suspend fun clearAll() = targetDao.deleteAll()
}
