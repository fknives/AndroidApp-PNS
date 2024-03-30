package org.fnives.android.servernotifications.message

data class EncryptedMessage(
    val iv: String,
    val data: String,
    val sessionKey: String
)