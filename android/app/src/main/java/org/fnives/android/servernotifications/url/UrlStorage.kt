package org.fnives.android.servernotifications.url

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import org.fnives.android.servernotifications.BuildConfig

fun UrlStorage(context: Context): UrlStorage {
    val sharedPref = context.getSharedPreferences("url_storage", Context.MODE_PRIVATE)
    return UrlStorage(sharedPref)
}

class UrlStorage(private val sharedPref: SharedPreferences) {
    // DO NOT DO THIS: listen to the sharedPref or use DataStore
    private val update = MutableSharedFlow<Unit>(extraBufferCapacity = 1, replay = 1).apply {
        tryEmit(Unit)
    }
    private var _current get() = sharedPref.getString("current", null) ?: BuildConfig.BASE_URL
        set(value) {
            sharedPref.edit().putString("current", value).apply()
        }
    val currentUrl: Flow<String> = update.map {_current }
    val current: String get() = _current
    private var _urlOptions get() = sharedPref.getStringSet("all", null) ?: setOf(BuildConfig.BASE_URL)
        set(value) {
            sharedPref.edit().putStringSet("all", value).apply()
        }
    val urlOptions: Flow<Set<String>> = update.map {_urlOptions }

    fun selectedUrl(url: String) {
        _current = url
        _urlOptions = _urlOptions + url
        update.tryEmit(Unit)
    }
}