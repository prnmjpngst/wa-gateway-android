package com.aji.wa_gateway

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.aji.wa_gateway.util.LoggingUtil

class MainActivity : AppCompatActivity() {

    private lateinit var httpServer: HttpServer
    private var serverStarted = false

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            startServer()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        LoggingUtil.init()

        findViewById<Button>(R.id.btn_start).setOnClickListener {
            requestAccessibilityPermission()
        }

        findViewById<Button>(R.id.btn_open_web).setOnClickListener {
            val url = "http://localhost:${getServerPort()}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        if (savedInstanceState == null) {
            requestOverlayPermission()
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("WA Gateway needs overlay permission to start the web server.")
                .setPositiveButton("Grant") { _, _ ->
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    overlayPermissionLauncher.launch(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            startServer()
        }
    }

    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        Toast.makeText(this, "Enable 'WA Gateway' in Accessibility Settings", Toast.LENGTH_LONG).show()
    }

    private fun startServer() {
        if (serverStarted) return
        serverStarted = true
        httpServer = HttpServer(this)
        httpServer.start(getServerPort())
        LoggingUtil.info("Server started: http://localhost:${getServerPort()}")
    }

    private fun getServerPort(): Int {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        return prefs.getInt("server_port", 8888)
    }

    override fun onDestroy() {
        if (::httpServer.isInitialized) httpServer.stop()
        super.onDestroy()
    }
}
