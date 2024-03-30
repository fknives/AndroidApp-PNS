package org.fnives.android.servernotifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Space
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.fnives.android.servernotifications.message.MessageStorage
import org.fnives.android.servernotifications.message.Priority
import org.fnives.android.servernotifications.message.Priority.High
import org.fnives.android.servernotifications.message.Priority.Low
import org.fnives.android.servernotifications.message.Priority.Medium
import org.fnives.android.servernotifications.message.Priority.Undefined
import org.fnives.android.servernotifications.token.TokenWebView
import org.fnives.android.servernotifications.ui.theme.ServerNotificationsTheme
import org.fnives.android.servernotifications.url.UrlStorage

class MainActivity : ComponentActivity() {

    private val grantedEventFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else {
        null
    }
    private val grantedFlow = grantedEventFlow.onStart { emit(Unit) }
        .map {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.checkSelfPermission(
                    this,
                    notificationPermission!!
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _: Boolean ->
        println(grantedEventFlow.tryEmit(Unit))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val messageStorage = MessageStorage(this)
        val urlStorage = UrlStorage(this)
        val formatter = DateTimeFormatter.ofPattern("MMM d yyyy  hh:mm a")
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.of("UTC"))
        setContent {
            ServerNotificationsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var navigationState by remember { mutableStateOf(Navigation.Home) }
                    BackHandler(navigationState != Navigation.Home) {
                        navigationState = Navigation.Home
                    }

                    when (navigationState) {
                        Navigation.Administration -> {
                            TokenWebView(url = urlStorage.current)
                        }

                        Navigation.Home -> {
                            val grantedState by grantedFlow.collectAsState(initial = null)
                            if (grantedState == true) {
                                val scope = rememberCoroutineScope()
                                Column {
                                    AdministrationNotice(
                                        onShowRegistration = {
                                            navigationState = Navigation.Administration
                                        },
                                        onShowChangeUrl = {
                                            navigationState = Navigation.ChangeURL
                                        },
                                        onClearLogs = { scope.launch { messageStorage.clear() } })
                                    Logs(messageStorage, formatter)
                                }
                            } else if (grantedState == false) {
                                NoPermission()
                            }
                        }

                        Navigation.ChangeURL -> {
                            ChangeURL(urlStorage)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun NoPermission() {
        Box(modifier = Modifier.fillMaxSize()) {
            Button(
                modifier = Modifier.align(Alignment.Center),
                onClick = {
                    requestPermissionLauncher.launch(notificationPermission)
                }) {
                Text(text = "Grant notification permission")
            }
        }
    }

    @Composable
    fun AdministrationNotice(
        onShowRegistration: () -> Unit,
        onShowChangeUrl: () -> Unit,
        onClearLogs: () -> Unit,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), Arrangement.Center
        ) {
            Row {
                Button(
                    onClick = { onShowRegistration() },
                ) {
                    Text(text = "Administration")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = { onShowChangeUrl() },
                ) {
                    Text(text = "Change URL")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onClearLogs() }) {
                Text(text = "Clear logs")
            }
        }
    }

    @Composable
    fun Logs(messageStorage: MessageStorage, formatter: DateTimeFormatter) {
        val messages by messageStorage.messages.collectAsState(initial = listOf())
        LazyColumn {
            messages.forEach {
                item {
                    Text(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        text = "${it.priority.iconText()} ${it.serviceName}: ${it.message} at ${
                            formatter.format(
                                it.timestamp
                            )
                        }"
                    )
                }
            }
        }
    }
    @Composable
    fun ChangeURL(urlStorage: UrlStorage) {
        val current by urlStorage.currentUrl.collectAsState(initial = "")
        val options by urlStorage.urlOptions.collectAsState(initial = emptySet())
        var overwrite by remember { mutableStateOf("") }
        val keyboardOptions = remember{KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Done)}
        Column {
            Text(text = "Current", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = current, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Overwrite", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            TextField(
                overwrite,
                textStyle = MaterialTheme.typography.bodyLarge,
                onValueChange = { overwrite = it },
                keyboardActions = KeyboardActions(onDone = {
                    urlStorage.selectedUrl(overwrite)
                    defaultKeyboardAction(ImeAction.Done)
                }),
                keyboardOptions = keyboardOptions
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Recent", style = MaterialTheme.typography.labelMedium)
            options.forEach {
                Button(onClick = {
                    urlStorage.selectedUrl(it)
                }) {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

fun Priority.iconText(): String {
    return when (this) {
        High -> "\uD83D\uDEA8"
        Medium -> "⚠\uFE0F"
        Low -> "ℹ\uFE0F"
        Undefined -> "\uD83E\uDD14"
    }
}

enum class Navigation {
    Administration, Home, ChangeURL
}    