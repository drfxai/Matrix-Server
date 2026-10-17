package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.ServerPreferences
import com.example.service.MatrixServerService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.i("BootReceiver", "Device boot event received: $action")
            val prefs = ServerPreferences(context)
            val config = prefs.loadConfig()
            if (config.autoStartOnBoot) {
                Log.i("BootReceiver", "Auto-start on boot enabled. Starting MatrixServerService...")
                MatrixServerService.start(context)
            }
        }
    }
}
