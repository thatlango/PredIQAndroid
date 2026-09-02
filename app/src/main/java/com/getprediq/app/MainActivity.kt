package com.getprediq.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.getprediq.app.ui.theme.PrediqTheme

class MainActivity : ComponentActivity() {
    private var authCallback by androidx.compose.runtime.mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authCallback = intent?.data
        enableEdgeToEdge()
        setContent { PrediqTheme { PrediqAppV2(authCallback) { authCallback = null } } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        authCallback = intent.data
    }
}
