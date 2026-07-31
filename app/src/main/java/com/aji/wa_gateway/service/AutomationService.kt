package com.aji.wa_gateway.service

import android.content.Context
import com.aji.wa_gateway.WaAccessibilityService
import com.aji.wa_gateway.db.entity.AppConfig
import com.aji.wa_gateway.db.entity.SendHistory
import com.aji.wa_gateway.db.entity.Target
import com.aji.wa_gateway.repository.HistoryRepository
import com.aji.wa_gateway.repository.TargetRepository
import com.aji.wa_gateway.util.LoggingUtil
import kotlinx.coroutines.*
import kotlin.random.Random

class AutomationService(
    private val context: Context,
    private val targetRepository: TargetRepository,
    private val historyRepository: HistoryRepository,
    private val typingBehavior: TypingBehavior = TypingBehavior
) {
    companion object {
        const val STATE_IDLE = "idle"
        const val STATE_RUNNING = "running"
        const val STATE_PAUSED = "paused"
    }

    @Volatile var currentState: String = STATE_IDLE
        private set
    @Volatile var currentTarget: String = ""
        private set
    @Volatile var progress: Int = 0
        private set
    @Volatile var total: Int = 0
        private set

    private var job: Job? = null
    var onMessageGenerated: ((String) -> Unit)? = null
    var onStatusChanged: ((String) -> Unit)? = null

    fun sendAll(config: AppConfig, targets: List<Target>) {
        if (currentState == STATE_RUNNING) {
            LoggingUtil.warn("Already running, ignoring sendAll request")
            return
        }

        val waService = WaAccessibilityService.instance
        if (waService == null) {
            LoggingUtil.error("Accessibility service not enabled")
            return
        }

        job = CoroutineScope(Dispatchers.IO).launch {
            currentState = STATE_RUNNING
            onStatusChanged?.invoke(STATE_RUNNING)
            progress = 0
            total = targets.size

            val batchTargets = if (config.maxTargetsPerBatch > 0) {
                targets.take(config.maxTargetsPerBatch)
            } else targets

            for ((index, target) in batchTargets.withIndex()) {
                if (currentState == STATE_PAUSED) {
                    while (currentState == STATE_PAUSED) delay(500)
                    if (currentState != STATE_RUNNING) break
                }

                currentTarget = target.nomorHp
                LoggingUtil.info("Processing ${index + 1}/${batchTargets.size}: ${target.nomorHp}")

                val message = target.pesan.ifBlank {
                    MessageGenerator.generate(config.messageTemplate, target)
                }

                onMessageGenerated?.invoke(message)

                val result = sendMessage(target, message, config)

                val historyEntry = SendHistory(
                    nomorHp = target.nomorHp,
                    namaPemilik = target.namaPemilik,
                    status = result.status,
                    errorMessage = result.errorMessage,
                    attemptNumber = 1
                )
                historyRepository.insert(historyEntry)

                LoggingUtil.info("Result for ${target.nomorHp}: ${result.status}")
                progress = index + 1

                if (result.status == SendHistory.STATUS_ERROR && config.stopOnError) {
                    LoggingUtil.warn("Stop on error enabled, halting batch")
                    break
                }

                if (index < batchTargets.size - 1) {
                    val delaySec = Random.nextLong(
                        config.batchDelayMinSec.toLong(),
                        config.batchDelayMaxSec.toLong() + 1
                    )
                    LoggingUtil.info("Waiting ${delaySec}s before next target...")
                    delay(delaySec * 1000)
                }
            }

            currentState = STATE_IDLE
            currentTarget = ""
            onStatusChanged?.invoke(STATE_IDLE)
            LoggingUtil.info("Batch complete: $progress/$total sent")
        }
    }

    private suspend fun sendMessage(target: Target, message: String, config: AppConfig): WaAccessibilityService.Companion.SendResult {
        val waService = WaAccessibilityService.instance
        if (waService == null) {
            LoggingUtil.error("Accessibility service not enabled")
            return WaAccessibilityService.Companion.SendResult(SendHistory.STATUS_ERROR, "Accessibility service not enabled")
        }

        val normalizedNumber = target.nomorHp.replace(Regex("[^0-9]"), "")
        if (normalizedNumber.length < 5) {
            return WaAccessibilityService.Companion.SendResult(SendHistory.STATUS_INVALID_NUMBER, "Invalid phone number")
        }

        LoggingUtil.info("Opening WA: https://wa.me/$normalizedNumber")

        return suspendCancellableCoroutine { cont ->
            waService.onSendResult = { result ->
                if (cont.isActive) cont.resume(result)
            }
            waService.initiateSend(normalizedNumber, message, target.namaPemilik)
        }
    }

    fun pause() {
        if (currentState == STATE_RUNNING) {
            currentState = STATE_PAUSED
            onStatusChanged?.invoke(STATE_PAUSED)
        }
    }

    fun resume() {
        if (currentState == STATE_PAUSED) {
            currentState = STATE_RUNNING
            onStatusChanged?.invoke(STATE_RUNNING)
        }
    }

    fun stop() {
        job?.cancel()
        currentState = STATE_IDLE
        currentTarget = ""
        onStatusChanged?.invoke(STATE_IDLE)
    }
}
