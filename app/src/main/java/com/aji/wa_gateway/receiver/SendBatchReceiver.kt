package com.aji.wa_gateway.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aji.wa_gateway.util.LoggingUtil
import com.aji.wa_gateway.worker.SendBatchWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SendBatchReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        LoggingUtil.info("Scheduled send triggered")
        CoroutineScope(Dispatchers.IO).launch {
            SendBatchWorker(context).execute()
        }
    }
}
