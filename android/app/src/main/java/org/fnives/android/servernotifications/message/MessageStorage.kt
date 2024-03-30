package org.fnives.android.servernotifications.message

import android.content.Context
import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

fun MessageStorage(context: Context): MessageStorage {
    return MessageStorage(
        limit = 100,
        file = File(context.filesDir, "logs"),
        converter = Converter(),
    )
}

class MessageStorage(
    private val limit: Int,
    private val file: File,
    private val converter: Converter,
    private val context: CoroutineContext = Dispatchers.IO
) {
    init {
        require(limit > 0)
    }

    // DO NOT DO THIS: use database in prod
    val messages = writeEvent.onStart { emit(Unit) }.map {
        fileMutex.withLock {
            if (file.exists()) {
                file.readLines()
                    .reversed()
                    .mapNotNull(converter::deserialize)
            } else {
                listOf()
            }
        }
    }


    suspend fun addMessage(message: Message) {
        val serializedMessage = converter.serialize(message) ?: return
        withContext(context) {
            fileMutex.withLock {
                val lines = if (file.exists()) file.readLines() else listOf()
                val current = lines.takeLast(limit - 1)
                val allLogs = current + serializedMessage
                file.writeText(allLogs.joinToString("\n"))
            }
        }
        writeEvent.tryEmit(Unit)
    }

    suspend fun clear() {
        withContext(context) {
            fileMutex.withLock {
                file.delete()
            }
        }
        writeEvent.tryEmit(Unit)
    }

    companion object {
        val fileMutex = Mutex()
        private val writeEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    }
}