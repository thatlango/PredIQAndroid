package com.getprediq.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.getprediq.app.data.PrediqLiveStream
import com.getprediq.app.data.SessionStore
import com.getprediq.app.ui.theme.PrediqTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var authCallback by androidx.compose.runtime.mutableStateOf<Uri?>(null)
    private lateinit var contractViewModel: PrediqContractViewModel
    private lateinit var liveStream: PrediqLiveStream
    private var liveStreamJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contractViewModel = ViewModelProvider(
            this,
            PrediqContractViewModel.factory(applicationContext),
        )[PrediqContractViewModel::class.java]
        liveStream = PrediqLiveStream(
            SessionStore(applicationContext),
            onEvent = { event ->
                if (event.type in setOf("goal", "score_change", "status_change", "live_started", "analysis_updated")) {
                    runOnUiThread {
                        contractViewModel.loadLive()
                        if (event.type == "analysis_updated") contractViewModel.loadToday()
                    }
                }
            },
            onConnectionChanged = {},
        )
        consumeIntent(intent)
        requestNotificationPermissionIfNeeded()
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContent { PrediqTheme { PrediqContractApp(authCallback) { authCallback = null } } }
    }

    override fun onStart() {
        super.onStart()
        liveStreamJob?.cancel()
        liveStreamJob = lifecycleScope.launch {
            while (isActive) {
                liveStream.connect()
                delay(30_000)
            }
        }
    }

    override fun onStop() {
        liveStreamJob?.cancel()
        liveStreamJob = null
        liveStream.close()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIntent(intent)
    }

    private fun consumeIntent(intent: Intent?) {
        val uri = intent?.data
        authCallback = uri?.takeIf { it.scheme == "prediq" && it.host == "auth" }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST)
    }

    private companion object {
        const val NOTIFICATION_PERMISSION_REQUEST = 1001
    }
}