package com.aji.wa_gateway.util

import android.util.Log
import timber.log.Timber

object LoggingUtil {
    private val logBuffer = mutableListOf<LogEntry>()
    private val maxEntries = 1000
    private val listeners = mutableListOf<(LogEntry) -> Unit>()

    data class LogEntry(
        val level: Level,
        val message: String,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        enum class Level { INFO, WARNING, ERROR }
    }

    fun init() {
        Timber.plant(object : Timber.DebugTree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                val level = when (priority) {
                    Log.WARN -> LogEntry.Level.WARNING
                    Log.ERROR, Log.ASSERT -> LogEntry.Level.ERROR
                    else -> LogEntry.Level.INFO
                }
                addEntry(LogEntry(level, if (tag != null) "[$tag] $message" else message))
            }
        })
    }

    private fun addEntry(entry: LogEntry) {
        synchronized(logBuffer) {
            logBuffer.add(entry)
            if (logBuffer.size > maxEntries) logBuffer.removeFirst()
        }
        synchronized(listeners) {
            listeners.forEach { it(entry) }
        }
    }

    fun getLogs(): List<LogEntry> = synchronized(logBuffer) { logBuffer.toList() }

    fun clearLogs() = synchronized(logBuffer) { logBuffer.clear() }

    fun addListener(listener: (LogEntry) -> Unit) {
        synchronized(listeners) { listeners.add(listener) }
    }

    fun removeListener(listener: (LogEntry) -> Unit) {
        synchronized(listeners) { listeners.remove(listener) }
    }

    fun info(message: String) {
        Timber.i(message)
    }

    fun warn(message: String) {
        Timber.w(message)
    }

    fun error(message: String) {
        Timber.e(message)
    }

    fun redactSaKey(message: String): String {
        return message.replace(Regex("\"private_key\":\\s*\"[^\"]+\""), "\"private_key\":\"[REDACTED]\"")
            .replace(Regex("\"client_email\":\\s*\"[^\"]+\""), "\"client_email\":\"[REDACTED]\"")
    }
}
