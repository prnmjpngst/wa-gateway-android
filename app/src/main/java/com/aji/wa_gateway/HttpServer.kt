package com.aji.wa_gateway

import android.content.Context
import com.aji.wa_gateway.db.AppDatabase
import com.aji.wa_gateway.db.entity.AppConfig
import com.aji.wa_gateway.db.entity.SendHistory
import com.aji.wa_gateway.db.entity.Schedule
import com.aji.wa_gateway.repository.ConfigRepository
import com.aji.wa_gateway.repository.HistoryRepository
import com.aji.wa_gateway.repository.TargetRepository
import com.aji.wa_gateway.service.AutomationService
import com.aji.wa_gateway.service.GoogleSheetsService
import com.aji.wa_gateway.service.MessageGenerator
import com.aji.wa_gateway.util.EncryptionUtil
import com.aji.wa_gateway.util.LoggingUtil
import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.html.*
import java.io.File

class HttpServer(private val context: Context) {
    private val gson = Gson()
    private val db = AppDatabase.getInstance(context)
    private val configRepo = ConfigRepository(db.appConfigDao(), db.scheduleDao())
    private val targetRepo = TargetRepository(db.targetDao())
    private val historyRepo = HistoryRepository(db.sendHistoryDao())
    private val automationService = AutomationService(context, targetRepo, historyRepo)
    private val sheetsService = GoogleSheetsService(context)

    private var server: ApplicationEngine? = null
    private var logJobs = mutableListOf<Job>()

    fun start(port: Int = 8888) {
        if (server != null) return

        server = embeddedServer(CIO, port = port) {
            install(ContentNegotiation) { gson {} }
            install(WebSockets)

            routing {
                get("/") { serveWebUi(call) }
                get("/assets/{path...}") { serveAsset(call) }

                get("/api/config") {
                    val config = configRepo.getConfig()
                    call.respond(mapOf(
                        "saKeyConfigured" to EncryptionUtil.hasSaKey(this@HttpServer.context),
                        "sheetId" to (config.sheetId ?: ""),
                        "sheetTab" to config.sheetTab,
                        "messageTemplate" to config.messageTemplate,
                        "typingEnabled" to config.typingEnabled,
                        "typingMinDelay" to config.typingMinDelayMs,
                        "typingMaxDelay" to config.typingMaxDelayMs,
                        "typingPauseFreq" to config.typingPauseFreq,
                        "typingPauseDuration" to config.typingPauseDurationMs,
                        "presendDelay" to config.presendDelayMs,
                        "batchDelayMin" to config.batchDelayMinSec,
                        "batchDelayMax" to config.batchDelayMaxSec,
                        "maxTargetsPerBatch" to config.maxTargetsPerBatch,
                        "stopOnError" to config.stopOnError,
                        "maxRetries" to config.maxRetries,
                        "autoSyncHours" to config.autoSyncHours,
                        "lastSyncTimestamp" to config.lastSyncTimestamp,
                        "serverPort" to config.serverPort
                    ))
                }

                post("/api/config/sa-key") {
                    val body = call.receive<Map<String, String>>()
                    val saKeyJson = body["saKey"] ?: return@post call.respond(mapOf("error" to "Missing saKey"))
                    EncryptionUtil.saveSaKey(this@HttpServer.context, saKeyJson)
                    LoggingUtil.info("SA key saved (redacted)")
                    call.respond(mapOf("success" to true))
                }

                post("/api/config/sheet") {
                    val body = call.receive<Map<String, String>>()
                    val config = configRepo.getConfig()
                    configRepo.updateConfig(config.copy(
                        sheetId = body["sheetId"] ?: config.sheetId,
                        sheetTab = body["sheetTab"] ?: config.sheetTab
                    ))
                    call.respond(mapOf("success" to true))
                }

                post("/api/config/message-template") {
                    val body = call.receive<Map<String, String>>()
                    val config = configRepo.getConfig()
                    configRepo.updateConfig(config.copy(messageTemplate = body["template"] ?: config.messageTemplate))
                    call.respond(mapOf("success" to true))
                }

                post("/api/config/typing-behavior") {
                    val body = call.receive<Map<String, Any>>()
                    val config = configRepo.getConfig()
                    configRepo.updateConfig(config.copy(
                        typingEnabled = (body["typingEnabled"] as? Boolean) ?: config.typingEnabled,
                        typingMinDelayMs = (body["typingMinDelay"] as? Number)?.toInt() ?: config.typingMinDelayMs,
                        typingMaxDelayMs = (body["typingMaxDelay"] as? Number)?.toInt() ?: config.typingMaxDelayMs,
                        typingPauseFreq = (body["typingPauseFreq"] as? Number)?.toInt() ?: config.typingPauseFreq,
                        typingPauseDurationMs = (body["typingPauseDuration"] as? Number)?.toInt() ?: config.typingPauseDurationMs,
                        presendDelayMs = (body["presendDelay"] as? Number)?.toInt() ?: config.presendDelayMs
                    ))
                    call.respond(mapOf("success" to true))
                }

                post("/api/config/schedule") {
                    val body = call.receive<Map<String, Any>>()
                    val schedules = configRepo.getSchedules()
                    val existingSchedule = schedules.firstOrNull() ?: Schedule()
                    configRepo.upsertSchedule(existingSchedule.copy(
                        enabled = (body["enabled"] as? Boolean) ?: existingSchedule.enabled,
                        daysOfWeek = (body["daysOfWeek"] as? String) ?: existingSchedule.daysOfWeek,
                        timeOfDay = (body["timeOfDay"] as? String) ?: existingSchedule.timeOfDay
                    ))
                    call.respond(mapOf("success" to true))
                }

                get("/api/targets") {
                    val count = targetRepo.count()
                    val config = configRepo.getConfig()
                    call.respond(mapOf(
                        "count" to count,
                        "lastSyncTimestamp" to (config.lastSyncTimestamp ?: 0)
                    ))
                }

                post("/api/targets/sync") {
                    try {
                        val config = configRepo.getConfig()
                        if (config.sheetId == null) {
                            call.respond(mapOf("error" to "Sheet ID not configured"))
                            return@post
                        }
                        val result = sheetsService.fetchTargets(config.sheetId, config.sheetTab)
                        result.fold(
                            onSuccess = { targets ->
                                val withMessages = MessageGenerator.generateForTargets(config.messageTemplate, targets)
                                targetRepo.clearAll()
                                targetRepo.insertAll(withMessages)
                                configRepo.updateConfig(config.copy(lastSyncTimestamp = System.currentTimeMillis()))
                                call.respond(mapOf("success" to true, "count" to targets.size))
                            },
                            onFailure = { e ->
                                call.respond(mapOf("error" to e.message))
                            }
                        )
                    } catch (e: Exception) {
                        call.respond(mapOf("error" to e.message))
                    }
                }

                post("/api/send/now") {
                    try {
                        val config = configRepo.getConfig()
                        val targets = targetRepo.getAll()
                        if (targets.isEmpty()) {
                            call.respond(mapOf("error" to "No targets to send"))
                            return@post
                        }
                        automationService.sendAll(config, targets)
                        call.respond(mapOf("success" to true, "targets" to targets.size))
                    } catch (e: Exception) {
                        call.respond(mapOf("error" to e.message))
                    }
                }

                get("/api/send/status") {
                    call.respond(mapOf(
                        "state" to automationService.currentState,
                        "currentTarget" to automationService.currentTarget,
                        "progress" to automationService.progress,
                        "total" to automationService.total
                    ))
                }

                get("/api/history") {
                    val page = call.parameters["page"]?.toIntOrNull() ?: 1
                    val status = call.parameters["status"]
                    val records = if (status != null) {
                        historyRepo.getPageByStatus(status, page)
                    } else {
                        historyRepo.getPage(page)
                    }
                    val totalCount = if (status != null) historyRepo.countByStatus(status) else historyRepo.count()
                    call.respond(mapOf(
                        "records" to records,
                        "total" to totalCount,
                        "page" to page
                    ))
                }

                delete("/api/history/{id}") {
                    val id = call.parameters["id"]?.toLongOrNull()
                    if (id != null) {
                        historyRepo.deleteById(id)
                        call.respond(mapOf("success" to true))
                    } else {
                        call.respond(mapOf("error" to "Invalid id"))
                    }
                }

                get("/api/logs") {
                    val logs = LoggingUtil.getLogs()
                    call.respond(mapOf("logs" to logs.map {
                        mapOf("level" to it.level.name, "message" to it.message, "timestamp" to it.timestamp)
                    }))
                }

                webSocket("/ws/logs") {
                    val logListener: (LoggingUtil.LogEntry) -> Unit = { entry ->
                        launch {
                            try {
                                send(Frame.Text(gson.toJson(mapOf(
                                    "level" to entry.level.name,
                                    "message" to entry.message,
                                    "timestamp" to entry.timestamp
                                ))))
                            } catch (_: Exception) {}
                        }
                    }
                    LoggingUtil.addListener(logListener)
                    try {
                        for (frame in incoming) {}
                    } catch (_: Exception) {}
                    finally {
                        LoggingUtil.removeListener(logListener)
                    }
                }

                get("/api/sheets/test") {
                    val config = configRepo.getConfig()
                    if (config.sheetId == null) {
                        call.respond(mapOf("error" to "Sheet ID not configured"))
                        return@get
                    }
                    val result = sheetsService.testConnection(config.sheetId, config.sheetTab)
                    result.fold(
                        onSuccess = { columns ->
                            call.respond(mapOf("success" to true, "columns" to columns))
                        },
                        onFailure = { e ->
                            call.respond(mapOf("error" to e.message))
                        }
                    )
                }
            }
        }
        server?.start(wait = false)
        LoggingUtil.info("HTTP server started on port $port")
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
        logJobs.forEach { it.cancel() }
        logJobs.clear()
    }

    private suspend fun serveWebUi(call: ApplicationCall) {
        try {
            val html = context.assets.open("web_ui/index.html").bufferedReader().use { it.readText() }
            call.respondText(html, ContentType.Text.Html)
        } catch (e: Exception) {
            call.respondHtml {
                head { title("WA Gateway") }
                body {
                    h1 { +"WA Gateway" }
                    p { +"Web UI not found. Build the React app first." }
                }
            }
        }
    }

    private suspend fun serveAsset(call: ApplicationCall) {
        val path = call.parameters["path"] ?: return call.respondText("Not found", status = HttpStatusCode.NotFound)
        try {
            val fullPath = "web_ui/$path"
            val bytes = context.assets.open(fullPath).readBytes()
            val contentType = when {
                path.endsWith(".js") -> ContentType.Application.JavaScript
                path.endsWith(".css") -> ContentType.Text.CSS
                path.endsWith(".png") -> ContentType.Image.PNG
                path.endsWith(".svg") -> ContentType.Image.SVG
                path.endsWith(".ico") -> ContentType.Image.XIcon
                else -> ContentType.Application.OctetStream
            }
            call.respondBytes(bytes, contentType)
        } catch (e: Exception) {
            call.respondText("Not found", status = HttpStatusCode.NotFound)
        }
    }
}
