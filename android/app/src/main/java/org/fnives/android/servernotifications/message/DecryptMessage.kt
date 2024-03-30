package org.fnives.android.servernotifications.message

import android.content.Context
import org.fnives.android.servernotifications.BuildConfig
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


interface DecryptMessage {

    fun decrypt(message: EncryptedMessage): String?
}

fun DecryptMessage(context: Context): DecryptMessage {
    return DecryptMessageImpl(getKeyPair(context))
}

fun getKeyPair(context: Context): KeyPair {
    val publicKeyFile = File(context.filesDir, "key.pub")
    val privateKeyFile = File(context.filesDir, "key.rsa")
    val publicKeyBytes = if (publicKeyFile.exists()) publicKeyFile.readBytes() else ByteArray(0)
    val privateKeyBytes = if (privateKeyFile.exists()) privateKeyFile.readBytes() else ByteArray(0)

    val keyPair: KeyPair
    if (publicKeyBytes.isEmpty() || privateKeyBytes.isEmpty()) {
        keyPair = DecryptMessageImpl.generateKeyPair()
        publicKeyFile.writeBytes(keyPair.public.encoded)
        privateKeyFile.writeBytes(keyPair.private.encoded)
    } else {
        val publicKey =
            KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(publicKeyBytes))
        val privateKey =
            KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(privateKeyBytes))

        keyPair = KeyPair(publicKey, privateKey)
    }

    return keyPair
}

class DecryptMessageImpl(private val keyPair: KeyPair) : DecryptMessage {

    //from Crypto.PublicKey import RSA
    //from Crypto.Cipher import AES, PKCS1_OAEP
    //
    //private_key = RSA.import_key(open("private.pem").read())
    //
    //with open("encrypted_data.bin", "rb") as f:
    //    enc_session_key = f.read(private_key.size_in_bytes())
    //    nonce = f.read(16)
    //    tag = f.read(16)
    //    ciphertext = f.read()
    //
    //# Decrypt the session key with the private RSA key
    //cipher_rsa = PKCS1_OAEP.new(private_key)
    //session_key = cipher_rsa.decrypt(enc_session_key)
    //
    //# Decrypt the data with the AES session key
    //cipher_aes = AES.new(session_key, AES.MODE_EAX, nonce)
    //data = cipher_aes.decrypt_and_verify(ciphertext, tag)
    //print(data.decode("utf-8"))
    @OptIn(ExperimentalEncodingApi::class)
    override fun decrypt(message: EncryptedMessage): String? {
        try {
            val privateKey = keyPair.private
            val rsaCipher = Cipher.getInstance("RSA/NONE/OAEPPadding")
            rsaCipher.init(Cipher.DECRYPT_MODE, privateKey)
            val sessionKey = rsaCipher.doFinal(Base64.decode(message.sessionKey))
            val iv = rsaCipher.doFinal(Base64.decode(message.iv))

            val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
            val aesKey = SecretKeySpec(sessionKey, "AES")
            cipher.init(Cipher.DECRYPT_MODE, aesKey, IvParameterSpec(iv))
            val decryptedByteArray = cipher.doFinal(Base64.decode(message.data))
            return decryptedByteArray.toString(StandardCharsets.UTF_8)
        } catch(e: Throwable) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            return null
        }
    }

    companion object {

        fun generateKeyPair(): KeyPair {
            val kpg = KeyPairGenerator.getInstance("RSA")
            kpg.initialize(2048)
            return kpg.genKeyPair()
        }
    }
}