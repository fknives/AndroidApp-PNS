package org.fnives.android.servernotifications.message

import androidx.annotation.Keep
import com.google.firebase.BuildConfig
import java.time.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

@Serializable
data class Message(
    @SerialName("message")
    val message: String,
    @SerialName("priority")
    val priority: Priority,
    @SerialName("service")
    val serviceName: String,
    @SerialName("time")
    @Serializable(InstantSerializer::class)
    val timestamp: Instant
)

@Keep
enum class Priority {
    High, Medium, Low, Undefined
}

@Suppress("FunctionName")
fun PriorityOf(priority: String?): Priority {
    return Priority.entries.firstOrNull { it.name.equals(priority, ignoreCase = true) }
        ?: Priority.Undefined
}

object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("java.time.Instant", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
}

interface Converter {

    fun serialize(message: Message): String?
    fun deserialize(message: String): Message?
}

fun Converter(): Converter {
    return SerializerConverter()
}

class SerializerConverter : Converter {
    override fun serialize(message: Message): String? =
        safeJson {
            Json.encodeToString(message)
        }

    override fun deserialize(message: String): Message? =
        safeJson {
            Json.decodeFromString<Message>(message)
        }

    private inline fun <T> safeJson(action: () -> T): T? {
        return try {
            action()
        } catch (parsingException: Throwable) {
            if (BuildConfig.DEBUG) {
                parsingException.printStackTrace()
            }
            null
        }
    }
}