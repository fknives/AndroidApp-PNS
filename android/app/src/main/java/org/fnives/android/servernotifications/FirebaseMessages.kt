package org.fnives.android.servernotifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.fnives.android.servernotifications.message.DecryptMessage
import org.fnives.android.servernotifications.message.EncryptedMessage
import org.fnives.android.servernotifications.message.Message
import org.fnives.android.servernotifications.message.MessageStorage
import org.fnives.android.servernotifications.message.Priority
import org.fnives.android.servernotifications.message.Priority.High
import org.fnives.android.servernotifications.message.Priority.Low
import org.fnives.android.servernotifications.message.Priority.Medium
import org.fnives.android.servernotifications.message.Priority.Undefined
import org.fnives.android.servernotifications.message.PriorityOf

class FirebaseMessages : FirebaseMessagingService() {

    private val decryptMessage by lazy { DecryptMessage(this) }
    private val messageStorage by lazy { MessageStorage(this) }
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        showNotification(
            Message(
                priority = High,
                serviceName = "PN Token has changed!",
                timestamp = Instant.now(),
                message = "",
            )
        )
    }

    override fun onMessageSent(msgId: String) {
        super.onMessageSent(msgId)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        println("received message")
        val sessionKey = remoteMessage.data["session_key"]
        val iv = remoteMessage.data["iv"]
        val messageData = remoteMessage.data["message"]
        val priority = PriorityOf(remoteMessage.data["priority"])
        val serviceName = remoteMessage.data["service"] ?: "Undefined"

        val encryptedMessage = EncryptedMessage(
            iv = iv ?: return log("IV not found in message"),
            sessionKey = sessionKey ?: return log("SessionKey not found in message"),
            data = messageData ?: return log("MessageData not found in message"),
        )

        coroutineScope.launch {
            val decrypted = decryptMessage.decrypt(encryptedMessage) ?: return@launch
            val message = Message(
                message = decrypted,
                priority = priority,
                serviceName = serviceName,
                timestamp = Instant.now()
            )
            messageStorage.addMessage(message)
            showNotification(message)
        }
    }

    private fun showNotification(message: Message) {
        val importance = when (message.priority) {
            High -> NotificationCompat.PRIORITY_HIGH
            Medium -> NotificationCompat.PRIORITY_DEFAULT
            Low -> NotificationCompat.PRIORITY_LOW
            Undefined -> NotificationCompat.PRIORITY_DEFAULT
        }
        showNotification(
            importance = importance,
            channelId = message.priority.getChannelId(),
            title = "${message.priority.iconText()} ${message.serviceName}",
            id = message.hashCode()
        )
    }

    private fun Priority.getChannelId(): String {
        val importance = when (this) {
            High -> NotificationManager.IMPORTANCE_HIGH
            Medium -> NotificationManager.IMPORTANCE_DEFAULT
            Low -> NotificationManager.IMPORTANCE_LOW
            Undefined -> NotificationManager.IMPORTANCE_DEFAULT
        }
        createChannel(
            name = name,
            description = "$name priority status updates from server",
            importance = importance,
            id = name
        )

        return name
    }

    private fun showNotification(
        importance: Int, title: String, channelId: String, id: Int,
        intent: Intent = Intent(this, MainActivity::class.java),
    ) {
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_server)
            .setContentTitle(title)
            .setPriority(importance)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setAutoCancel(true)

        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this).notify(id, builder.build())
        }
    }

    private fun createChannel(
        name: String,
        description: String,
        id: String,
        importance: Int = NotificationManager.IMPORTANCE_DEFAULT
    ) {
        val channel = NotificationChannel(id, name, importance).apply {
            this.description = description
        }
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun log(message: String) {
        Log.d("FirebaseMessages", message)
    }
}

