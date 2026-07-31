package com.aji.wa_gateway.repository

import com.aji.wa_gateway.db.dao.AppConfigDao
import com.aji.wa_gateway.db.dao.ScheduleDao
import com.aji.wa_gateway.db.entity.AppConfig
import com.aji.wa_gateway.db.entity.Schedule
import com.aji.wa_gateway.util.EncryptionUtil

class ConfigRepository(
    private val appConfigDao: AppConfigDao,
    private val scheduleDao: ScheduleDao
) {
    suspend fun getConfig(): AppConfig = appConfigDao.get() ?: AppConfig()

    suspend fun updateConfig(config: AppConfig) = appConfigDao.upsert(config)

    suspend fun getSchedules(): List<Schedule> = scheduleDao.getAll()

    suspend fun upsertSchedule(schedule: Schedule) = scheduleDao.upsert(schedule)

    suspend fun deleteSchedule(id: Long) = scheduleDao.deleteById(id)
}
