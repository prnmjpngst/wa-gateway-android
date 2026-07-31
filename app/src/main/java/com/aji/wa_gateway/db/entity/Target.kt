package com.aji.wa_gateway.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "targets")
data class Target(
    @PrimaryKey val nomorHp: String,
    val namaPemilik: String = "",
    val nomorKendaraan: String = "",
    val masaBerlaku: String = "",
    val pesan: String = "",
    val fetchTimestamp: Long = System.currentTimeMillis()
)
