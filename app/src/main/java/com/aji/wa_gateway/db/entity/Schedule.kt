package com.aji.wa_gateway.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class Schedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val enabled: Boolean = false,
    val daysOfWeek: String = "1,2,3,4,5",
    val timeOfDay: String = "08:00",
    val lastRun: Long? = null,
    val nextRun: Long? = null
)
