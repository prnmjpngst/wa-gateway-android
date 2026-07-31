package com.aji.wa_gateway.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "send_history")
data class SendHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nomorHp: String,
    val namaPemilik: String = "",
    val status: String,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val attemptNumber: Int = 1
) {
    companion object {
        const val STATUS_SENT = "sent"
        const val STATUS_INVALID_NUMBER = "invalid_number"
        const val STATUS_FAILED = "failed"
        const val STATUS_ERROR = "error"
    }
}
