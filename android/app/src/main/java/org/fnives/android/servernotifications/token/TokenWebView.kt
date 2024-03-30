package org.fnives.android.servernotifications.token

import android.R.attr.capitalize
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.Keep
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.messaging.FirebaseMessaging
import java.util.Locale
import java.util.concurrent.CountDownLatch
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.fnives.android.servernotifications.message.getKeyPair


@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TokenWebView(url: String) {
    var showLoading by remember(url) { mutableStateOf(true) }
    LaunchedEffect(url) {
        // can't be bothered for a real loading indicator
        delay(5000L)
        showLoading = false
    }
    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(0)
                    layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    settings.javaScriptEnabled = true
                    webViewClient = WebViewClient()
                    addJavascriptInterface(ReadTokenInterface(context), "Android")

                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.setSupportZoom(true)
                }
            },
            update = { webView ->
                webView.loadUrl(url)
            }
        )
        if (showLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Keep
class ReadTokenInterface(private val context: Context) {

    @OptIn(ExperimentalEncodingApi::class)
    @JavascriptInterface
    fun publicKey(): String {
        val publicKey = getKeyPair(context).public.encoded
        return Base64.encode(publicKey)
    }

    @JavascriptInterface
    fun messagingToken(): String {
        val latch = CountDownLatch(1)
        var token: String? = null
        FirebaseMessaging.getInstance().token.addOnSuccessListener {
            token = it
            latch.countDown()
        }
        latch.await()
        return token!!
    }

    @JavascriptInterface
    fun deviceName(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL

        return if (model.startsWith(manufacturer)) {
            model
        } else {
            "$manufacturer $model"
        }
    }
}