package com.netproxy.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var etPort: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnTurnOn: TextView
    private lateinit var btnTurnOff: TextView
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Provera i zahtev za dozvolu za notifikacije (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }

        etPort = findViewById(R.id.etPort)
        etPassword = findViewById(R.id.etPassword)
        btnTurnOn = findViewById(R.id.btnTurnOn)
        btnTurnOff = findViewById(R.id.btnTurnOff)
        tvStatus = findViewById(R.id.tvStatus)

        updateStatusUI(ProxyService.isServerRunning, ProxyService.activePort)

        btnTurnOn.setOnClickListener {
            startProxyServerManual()
        }

        btnTurnOff.setOnClickListener {
            stopProxyServerManual()
        }

        handleNetProxyIntent(intent)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            setIntent(intent)
            handleNetProxyIntent(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatusUI(ProxyService.isServerRunning, ProxyService.activePort)
    }

    private fun handleNetProxyIntent(intent: Intent) {
        if (Intent.ACTION_VIEW == intent.action) {
            val uri: Uri? = intent.data
            if (uri != null && uri.scheme == "proxy") {
                var startParam = uri.getQueryParameter("start")

                if (startParam == null) {
                    val hostOrPath = uri.host ?: uri.schemeSpecificPart
                    if (hostOrPath != null && hostOrPath.contains("start=")) {
                        val parts = hostOrPath.replace("//", "").split("=")
                        if (parts.size > 1) {
                            startParam = parts[1]
                        }
                    }
                }

                val portStr = etPort.text.toString().trim()
                val passwordStr = etPassword.text.toString().trim()
                val port = if (portStr.isNotEmpty()) portStr.toInt() else 8080
                val password = if (passwordStr.isNotEmpty()) passwordStr else "7777"

                val serviceIntent = Intent(this, ProxyService::class.java).apply {
                    putExtra("PORT", port)
                    putExtra("PASSWORD", password)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }

                updateStatusUI(true, port)

                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        finish()
                        moveTaskToBack(true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, 500)
            }
        }
    }

    private fun startProxyServerManual() {
        val portStr = etPort.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val port = if (portStr.isNotEmpty()) portStr.toInt() else 8080
        val passwordFinal = if (password.isNotEmpty()) password else "7777"

        val serviceIntent = Intent(this, ProxyService::class.java).apply {
            putExtra("PORT", port)
            putExtra("PASSWORD", passwordFinal)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        updateStatusUI(true, port)
    }

    private fun stopProxyServerManual() {
        val serviceIntent = Intent(this, ProxyService::class.java)
        stopService(serviceIntent)
        updateStatusUI(false)
    }

    private fun updateStatusUI(isRunning: Boolean, port: Int = 8080) {
        if (isRunning) {
            tvStatus.text = "Status: RUNNING (127.0.0.1:$port)"
            tvStatus.setTextColor(Color.parseColor("#1DB954"))
            etPort.isEnabled = false
            etPassword.isEnabled = false
        } else {
            tvStatus.text = "Status: OFF"
            tvStatus.setTextColor(Color.parseColor("#E50914"))
            etPort.isEnabled = true
            etPassword.isEnabled = true
        }
    }
}
