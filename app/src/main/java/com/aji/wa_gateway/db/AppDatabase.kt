package com.aji.wa_gateway.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aji.wa_gateway.db.dao.AppConfigDao
import com.aji.wa_gateway.db.dao.ScheduleDao
import com.aji.wa_gateway.db.dao.SendHistoryDao
import com.aji.wa_gateway.db.dao.TargetDao
import com.aji.wa_gateway.db.entity.AppConfig
import com.aji.wa_gateway.db.entity.Schedule
import com.aji.wa_gateway.db.entity.SendHistory
import com.aji.wa_gateway.db.entity.Target

@Database(
    entities = [AppConfig::class, Target::class, SendHistory::class, Schedule::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appConfigDao(): AppConfigDao
    abstract fun targetDao(): TargetDao
    abstract fun sendHistoryDao(): SendHistoryDao
    abstract fun scheduleDao(): ScheduleDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wa_gateway.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
