package com.getprediq.app

import android.content.Context
import com.getprediq.app.data.AccountFeatureRepository
import com.getprediq.app.data.SessionStore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val PUSH_PREFS = "prediq_push"
private const val PUSH_TOKEN = "token"

object PushRegistrationCoordinator {
    private fun prefs(context: Context) = context.getSharedPreferences(PUSH_PREFS, Context.MODE_PRIVATE)

    fun storeToken(context: Context, token: String) {
        if (token.isNotBlank()) prefs(context).edit().putString(PUSH_TOKEN, token).apply()
    }

    fun cachedToken(context: Context): String? = prefs(context).getString(PUSH_TOKEN, null)?.takeIf { it.isNotBlank() }

    private fun firebaseConfigured(context: Context): Boolean =
        context.resources.getIdentifier("google_app_id", "string", context.packageName) != 0

    suspend fun sync(context: Context) {
        val appContext = context.applicationContext
        if (SessionStore(appContext).accessToken.first().isNullOrBlank()) return
        val token = cachedToken(appContext) ?: if (firebaseConfigured(appContext)) currentFirebaseToken() else null
        if (token.isNullOrBlank()) return
        storeToken(appContext, token)
        runCatching { AccountFeatureRepository(appContext).registerDevice(token) }
    }

    suspend fun deactivate(context: Context) {
        val appContext = context.applicationContext
        val token = cachedToken(appContext) ?: return
        runCatching { AccountFeatureRepository(appContext).deactivateDevice(token) }
    }

    private suspend fun currentFirebaseToken(): String? = suspendCancellableCoroutine { continuation ->
        runCatching {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (continuation.isActive) continuation.resume(if (task.isSuccessful) task.result else null)
            }
        }.onFailure {
            if (continuation.isActive) continuation.resume(null)
        }
    }
}

class PrediqFirebaseMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        PushRegistrationCoordinator.storeToken(applicationContext, token)
        scope.launch { PushRegistrationCoordinator.sync(applicationContext) }
    }
}
