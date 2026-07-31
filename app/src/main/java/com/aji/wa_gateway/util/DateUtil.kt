package com.aji.wa_gateway.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateUtil {
    private val dateFormats = listOf(
        "yyyy-MM-dd",
        "dd/MM/yyyy",
        "MM/dd/yyyy",
        "yyyy/MM/dd",
        "dd-MM-yyyy",
        "d MMMM yyyy",
        "MMMM d, yyyy"
    )

    fun parseMasaBerlaku(dateStr: String): Date? {
        for (format in dateFormats) {
            try {
                val sdf = SimpleDateFormat(format, Locale("id", "ID"))
                sdf.isLenient = false
                return sdf.parse(dateStr.trim())
            } catch (_: Exception) {}
        }
        return null
    }

    fun calculateHitungHari(masaBerlakuStr: String): Long {
        val expiryDate = parseMasaBerlaku(masaBerlakuStr) ?: return 0
        val now = Date()
        val diffMs = expiryDate.time - now.time
        val days = TimeUnit.MILLISECONDS.toDays(diffMs)
        return if (days < 0) 0 else days
    }

    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
