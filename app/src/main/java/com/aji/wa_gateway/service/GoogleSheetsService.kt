package com.aji.wa_gateway.service

import android.content.Context
import com.aji.wa_gateway.db.entity.Target
import com.aji.wa_gateway.util.EncryptionUtil
import com.aji.wa_gateway.util.LoggingUtil
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.ServiceAccountCredentials
import java.io.ByteArrayInputStream

class GoogleSheetsService(private val context: Context) {

    private fun getSheetsService(): Sheets? {
        val saKeyJson = EncryptionUtil.getSaKey(context) ?: return null
        return try {
            val credentials = ServiceAccountCredentials.fromStream(ByteArrayInputStream(saKeyJson.toByteArray()))
                .createScoped(listOf(SheetsScopes.SPREADSHEETS_READONLY))
            val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
            Sheets.Builder(httpTransport, GsonFactory.getDefaultInstance(), HttpCredentialsAdapter(credentials))
                .setApplicationName("WA Gateway")
                .build()
        } catch (e: Exception) {
            LoggingUtil.error("Failed to create Sheets service: ${e.message}")
            null
        }
    }

    fun testConnection(sheetId: String, sheetTab: String): Result<List<String>> {
        return try {
            val service = getSheetsService() ?: return Result.failure(Exception("SA Key not configured"))
            val response = service.spreadsheets().values()
                .get(sheetId, "$sheetTab!1:1")
                .execute()
            val headerRow = response.getValues()?.firstOrNull()
                ?: return Result.failure(Exception("Empty sheet or invalid tab name"))
            val columns = headerRow.map { it.toString() }
            Result.success(columns)
        } catch (e: Exception) {
            LoggingUtil.error("Sheets test connection failed: ${e.message}")
            Result.failure(e)
        }
    }

    fun fetchTargets(sheetId: String, sheetTab: String): Result<List<Target>> {
        return try {
            val service = getSheetsService() ?: return Result.failure(Exception("SA Key not configured"))
            val response = service.spreadsheets().values()
                .get(sheetId, sheetTab)
                .execute()
            val rows = response.getValues() ?: return Result.success(emptyList())
            if (rows.size < 2) return Result.success(emptyList())

            val headerRow = rows.first().map { it.toString().trim().uppercase() }
            val nomorHpIdx = headerRow.indexOfFirst { it == "NOMOR_HP" }
            val namaIdx = headerRow.indexOfFirst { it == "NAMA_PEMILIK" }
            val nomorKendaraanIdx = headerRow.indexOfFirst { it == "NOMOR_KENDARAAN" }
            val masaBerlakuIdx = headerRow.indexOfFirst { it == "MASA_BERLAKU" }

            val targets = rows.drop(1).mapNotNull { row ->
                val nomorHp = row.getOrNull(nomorHpIdx)?.toString()?.trim() ?: return@mapNotNull null
                if (nomorHp.isBlank()) return@mapNotNull null
                Target(
                    nomorHp = nomorHp,
                    namaPemilik = namaIdx.takeIf { it >= 0 }?.let { row.getOrNull(it)?.toString()?.trim() } ?: "",
                    nomorKendaraan = nomorKendaraanIdx.takeIf { it >= 0 }?.let { row.getOrNull(it)?.toString()?.trim() } ?: "",
                    masaBerlaku = masaBerlakuIdx.takeIf { it >= 0 }?.let { row.getOrNull(it)?.toString()?.trim() } ?: ""
                )
            }
            Result.success(targets)
        } catch (e: Exception) {
            LoggingUtil.error("Sheets fetch failed: ${e.message}")
            Result.failure(e)
        }
    }
}
