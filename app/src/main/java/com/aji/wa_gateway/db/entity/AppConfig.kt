package com.aji.wa_gateway.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_config")
data class AppConfig(
    @PrimaryKey val id: Int = 1,
    val saKeyEncrypted: String? = null,
    val sheetId: String? = null,
    val sheetTab: String = "Sheet1",
    val messageTemplate: String = "Yth. Bapak/Ibu {nama}, KIR kendaraan Anda dengan nomor polisi {nomor_kendaraan} akan berakhir dalam {hitung_hari} hari ({masa_berlaku}). Mohon segera lakukan uji ulang di UPT PKB Kabupaten Lumajang.",
    val typingEnabled: Boolean = true,
    val typingMinDelayMs: Int = 60,
    val typingMaxDelayMs: Int = 150,
    val typingPauseFreq: Int = 7,
    val typingPauseDurationMs: Int = 600,
    val presendDelayMs: Int = 1000,
    val batchDelayMinSec: Int = 8,
    val batchDelayMaxSec: Int = 20,
    val maxTargetsPerBatch: Int = 0,
    val stopOnError: Boolean = false,
    val maxRetries: Int = 1,
    val autoSyncHours: Int = 24,
    val lastSyncTimestamp: Long? = null,
    val serverPort: Int = 8888
)
