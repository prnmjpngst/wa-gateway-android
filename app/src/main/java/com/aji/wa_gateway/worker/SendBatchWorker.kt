package com.aji.wa_gateway.worker

import android.content.Context
import com.aji.wa_gateway.db.AppDatabase
import com.aji.wa_gateway.repository.ConfigRepository
import com.aji.wa_gateway.repository.TargetRepository
import com.aji.wa_gateway.repository.HistoryRepository
import com.aji.wa_gateway.service.AutomationService

class SendBatchWorker(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val configRepo = ConfigRepository(db.appConfigDao(), db.scheduleDao())
    private val targetRepo = TargetRepository(db.targetDao())
    private val historyRepo = HistoryRepository(db.sendHistoryDao())
    private val automationService = AutomationService(context, targetRepo, historyRepo)

    suspend fun execute() {
        val config = configRepo.getConfig()
        val targets = targetRepo.getAll()
        if (targets.isEmpty()) return
        automationService.sendAll(config, targets)
    }
}
