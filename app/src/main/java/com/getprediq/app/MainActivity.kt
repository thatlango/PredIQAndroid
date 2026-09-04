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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.getprediq.app.data.PrediqLiveStream
import com.getprediq.app.data.SessionStore
import com.getprediq.app.ui.v2.PrediqMainShell
import com.getprediq.app.ui.v2.theme.V2SurfacePrimary
import com.getprediq.app.ui.v2.theme.V2TextMuted
import com.getprediq.app.ui.v2.theme.V2Typography
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
        setContent {
            Column {
                Box(Modifier.weight(1f)) {
                    PrediqMainShell(authCallback) { authCallback = null }
                }
                Surface(color = V2SurfacePrimary) {
                    Text(
                        text = "A product of © Tuku-Tuku Innovation Labs",
                        style = V2Typography.labelSmall,
                        color = V2TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
            }
        }
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
