package com.getprediq.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.getprediq.app.data.AccountFeatureRepository
import com.getprediq.app.data.SessionStore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val PUSH_PREFS = "prediq_push"
private const val PUSH_TOKEN = "token"
private const val ALERTS_CHANNEL_ID = "prediq_alerts"
const val PREDIQ_NOTIFICATION_ACTION = "com.getprediq.app.OPEN_NOTIFICATION"

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

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "PredIQ"
        val body = message.notification?.body ?: message.data["body"] ?: return
        ensureAlertsChannel()

        val openApp = Intent(this, MainActivity::class.java).apply {
            action = PREDIQ_NOTIFICATION_ACTION
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(this, 0, openApp, flags)
        val notification = NotificationCompat.Builder(this, ALERTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_prediq_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        getSystemService(NotificationManager::class.java).notify(message.messageId?.hashCode() ?: System.currentTimeMillis().toInt(), notification)
    }

    private fun ensureAlertsChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(ALERTS_CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(ALERTS_CHANNEL_ID, "PredIQ alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Important PredIQ prediction, live-change, and account alerts"
            }
        )
    }
}
