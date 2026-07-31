package com.aji.wa_gateway

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.aji.wa_gateway.db.AppDatabase
import com.aji.wa_gateway.db.entity.AppConfig
import com.aji.wa_gateway.db.entity.SendHistory
import com.aji.wa_gateway.repository.ConfigRepository
import com.aji.wa_gateway.repository.HistoryRepository
import com.aji.wa_gateway.service.TypingBehavior
import com.aji.wa_gateway.util.LoggingUtil
import kotlinx.coroutines.*

class WaAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: WaAccessibilityService? = null
            private set

        const val WA_PACKAGE = "com.whatsapp"
        const val WA_BUSINESS_PACKAGE = "com.whatsapp.w4b"
        const val SEND_BUTTON_ID = "com.whatsapp:id/send"
        const val TEXT_INPUT_ID = "com.whatsapp:id/entry"
        const val CHAT_CONTAINER_ID = "com.whatsapp:id/conversation_contact"
        const val INVALID_NUMBER_DIALOG = "com.whatsapp:id/alertTitle"
        const val TIMEOUT_MS = 5000L

        data class SendResult(
            val status: String,
            val errorMessage: String? = null
        )
    }

    var onSendResult: ((SendResult) -> Unit)? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val db by lazy { AppDatabase.getInstance(this) }
    private val configRepo by lazy { ConfigRepository(db.appConfigDao(), db.scheduleDao()) }
    private val historyRepo by lazy { HistoryRepository(db.sendHistoryDao()) }

    private var pendingMessage: String? = null
    private var currentTargetNumber: String? = null
    private var currentTargetName: String = ""
    private var state: AutomationState = AutomationState.IDLE
    private var timeoutJob: Job? = null

    enum class AutomationState { IDLE, LOADING_CHAT, TYPING, SENDING, DONE }

    private fun detectWaPackage(): String? {
        val candidates = listOf(WA_PACKAGE, WA_BUSINESS_PACKAGE)
        for (pkg in candidates) {
            try {
                packageManager.getPackageInfo(pkg, 0)
                return pkg
            } catch (_: Exception) {}
        }
        return null
    }

    fun initiateSend(phoneNumber: String, message: String, name: String = "") {
        pendingMessage = message
        currentTargetNumber = phoneNumber
        currentTargetName = name
        state = AutomationState.LOADING_CHAT
        openWhatsAppChat(phoneNumber)
    }

    private fun openWhatsAppChat(phoneNumber: String) {
        val waPackage = detectWaPackage()
        if (waPackage == null) {
            LoggingUtil.error("WhatsApp not installed")
            recordResult(SendResult(SendHistory.STATUS_ERROR, "WhatsApp not installed"))
            resetState()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("https://wa.me/$phoneNumber")
                `package` = waPackage
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
            startTimeout(TIMEOUT_MS) {
                LoggingUtil.error("Timeout waiting for chat to load")
                recordResult(SendResult(SendHistory.STATUS_FAILED, "Timeout waiting for chat"))
                resetState()
            }
        } catch (e: Exception) {
            LoggingUtil.error("Failed to open WhatsApp: ${e.message}")
            recordResult(SendResult(SendHistory.STATUS_ERROR, e.message))
            resetState()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val eventPackage = event.packageName?.toString() ?: return
        if (eventPackage != WA_PACKAGE && eventPackage != WA_BUSINESS_PACKAGE) return

        when (state) {
            AutomationState.LOADING_CHAT -> handleChatLoading(event)
            AutomationState.TYPING -> {} // handled by typeMessage
            AutomationState.SENDING -> handleSendResult(event)
            else -> {}
        }
    }

    private fun handleChatLoading(event: AccessibilityEvent) {
        val root = rootInActiveWindow ?: return

        val chatContainer = findNodeById(root, CHAT_CONTAINER_ID)
            ?: findNodeByText(root, "Type a message")
        if (chatContainer == null) {
            val invalidDialog = findNodeById(root, INVALID_NUMBER_DIALOG)
            if (invalidDialog != null) {
                LoggingUtil.warn("Invalid number detected: $currentTargetNumber")
                recordResult(SendHistory.STATUS_INVALID_NUMBER)
                resetState()
            }
            return
        }

        timeoutJob?.cancel()
        state = AutomationState.TYPING
        val message = pendingMessage ?: return

        serviceScope.launch {
            typeMessage(message)
        }
    }

    private suspend fun typeMessage(message: String) {
        val config = withContext(Dispatchers.IO) {
            configRepo.getConfig()
        }

        val root = rootInActiveWindow ?: return
        val inputField = findNodeById(root, TEXT_INPUT_ID)
            ?: findEditableNode(root)

        if (inputField == null) {
            LoggingUtil.error("Could not find text input field")
            recordResult(SendHistory.STATUS_FAILED, "Text input not found")
            resetState()
            return
        }

        if (!config.typingEnabled) {
            inputField.text = message
            inputField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT)
        } else {
            inputField.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            delay(300)

            val originalText = inputField.text?.toString() ?: ""
            val chars = message.toList()

            for ((index, char) in chars.withIndex()) {
                if (state != AutomationState.TYPING) break

                val charDelay = TypingBehavior.calculateDelayPerChar(
                    config.typingMinDelayMs, config.typingMaxDelayMs
                )
                delay(charDelay)

                val currentText = inputField.text?.toString() ?: originalText
                val newText = currentText + char
                inputField.text = newText
                inputField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT)

                if (TypingBehavior.shouldPause(index + 1, config.typingPauseFreq)) {
                    val pauseDuration = TypingBehavior.getPauseDuration(
                        config.typingPauseDurationMs, config.typingPauseDurationMs + 400
                    )
                    delay(pauseDuration)
                }
            }
        }

        delay(TypingBehavior.getPresendDelay(config.presendDelayMs))

        state = AutomationState.SENDING
        tapSendButton()
    }

    private fun tapSendButton() {
        val root = rootInActiveWindow ?: return
        val sendButton = findNodeById(root, SEND_BUTTON_ID)
            ?: findNodeByContentDescription(root, "Send")
            ?: findNodeByContentDescription(root, "Kirim")

        if (sendButton != null && sendButton.isClickable) {
            sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            startTimeout(3000) {
                LoggingUtil.warn("Timed out waiting for send confirmation")
                recordResult(SendHistory.STATUS_FAILED, "No confirmation after send")
                resetState()
            }
        } else {
            performTapOnCoordinates(root)
        }
    }

    private fun performTapOnCoordinates(root: AccessibilityNodeInfo) {
        val sendButtons = root.findAccessibilityNodeInfosByViewId(SEND_BUTTON_ID)
        val button = sendButtons?.firstOrNull { it.isClickable }
        if (button != null) {
            val rect = android.graphics.Rect()
            button.getBoundsInScreen(rect)
            val x = (rect.left + rect.right) / 2f
            val y = (rect.top + rect.bottom) / 2f
            val path = Path().apply {
                moveTo(x, y)
                lineTo(x, y)
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
                .build()
            dispatchGesture(gesture, null, null)
        } else {
            LoggingUtil.error("Send button not found")
            recordResult(SendHistory.STATUS_FAILED, "Send button not found")
            resetState()
        }
    }

    private fun handleSendResult(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val root = rootInActiveWindow ?: return
            val currentPackage = event.packageName?.toString() ?: ""

            if (currentPackage != WA_PACKAGE && currentPackage != WA_BUSINESS_PACKAGE || root.findAccessibilityNodeInfosByViewId(SEND_BUTTON_ID)?.isEmpty() == true) {
                timeoutJob?.cancel()
                recordResult(SendHistory.STATUS_SENT)
                LoggingUtil.info("Message sent successfully to $currentTargetNumber")
                state = AutomationState.DONE
                resetState()
            }
        }
    }

    override fun onInterrupt() {
        LoggingUtil.warn("Accessibility service interrupted")
        resetState()
    }

    private fun findNodeById(root: AccessibilityNodeInfo, id: String): AccessibilityNodeInfo? {
        val nodes = root.findAccessibilityNodeInfosByViewId(id)
        return nodes?.firstOrNull()
    }

    private fun findNodeByText(root: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val nodes = root.findAccessibilityNodeInfosByText(text)
        return nodes?.firstOrNull()
    }

    private fun findNodeByContentDescription(root: AccessibilityNodeInfo, desc: String): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node.contentDescription?.toString()?.lowercase() == desc.lowercase()) return node
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun findEditableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node.isEditable && node.isEnabled && node.isVisibleToUser) return node
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun startTimeout(timeoutMs: Long, onTimeout: () -> Unit) {
        timeoutJob?.cancel()
        timeoutJob = serviceScope.launch {
            delay(timeoutMs)
            onTimeout()
        }
    }

    private fun recordResult(status: String, error: String? = null) {
        val number = currentTargetNumber ?: return
        onSendResult?.invoke(SendResult(status, error))
        serviceScope.launch(Dispatchers.IO) {
            historyRepo.insert(
                SendHistory(
                    nomorHp = number,
                    namaPemilik = currentTargetName,
                    status = status,
                    errorMessage = error
                )
            )
        }
    }

    private fun resetState() {
        state = AutomationState.IDLE
        pendingMessage = null
        currentTargetNumber = null
        currentTargetName = ""
        timeoutJob?.cancel()
    }
}
