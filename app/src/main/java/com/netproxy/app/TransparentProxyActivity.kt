package com.netproxy.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class TransparentProxyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            handleIntent(intent)
        }
    }

    private fun handleIntent(incomingIntent: Intent) {
        if (Intent.ACTION_VIEW == incomingIntent.action) {
            val uri = incomingIntent.data
            if (uri != null && uri.scheme == "netproxy") {
                val port = 8080
                val password = "7777"

                val serviceIntent = Intent(this, ProxyService::class.java).apply {
                    putExtra("PORT", port)
                    putExtra("PASSWORD", password)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            }
        }
        finish()
    }
}
