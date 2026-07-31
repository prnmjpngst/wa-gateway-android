package com.aji.wa_gateway.worker

import android.content.Context
import com.aji.wa_gateway.db.AppDatabase
import com.aji.wa_gateway.repository.ConfigRepository
import com.aji.wa_gateway.repository.TargetRepository
import com.aji.wa_gateway.service.GoogleSheetsService
import com.aji.wa_gateway.service.MessageGenerator
import com.aji.wa_gateway.util.LoggingUtil

class SyncTargetsWorker(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val configRepo = ConfigRepository(db.appConfigDao(), db.scheduleDao())
    private val targetRepo = TargetRepository(db.targetDao())

    suspend fun sync(): Result<Int> {
        val config = configRepo.getConfig()
        val sheetId = config.sheetId ?: return Result.failure(Exception("Sheet ID not configured"))

        LoggingUtil.info("Starting sync from sheet: $sheetId")
        val sheetsService = GoogleSheetsService(context)
        val result = sheetsService.fetchTargets(sheetId, config.sheetTab)

        return result.map { targets ->
            val withMessages = MessageGenerator.generateForTargets(config.messageTemplate, targets)
            targetRepo.clearAll()
            targetRepo.insertAll(withMessages)

            val updatedConfig = config.copy(lastSyncTimestamp = System.currentTimeMillis())
            configRepo.updateConfig(updatedConfig)

            LoggingUtil.info("Sync complete: ${targets.size} targets fetched")
            targets.size
        }
    }
}
