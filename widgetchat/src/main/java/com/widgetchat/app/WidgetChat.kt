package com.widgetchat.app

import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.widgetchat.app.ui.ChatOptions
import com.widgetchat.app.ui.ChatScreen
import com.widgetchat.app.ui.ChatViewModel
import com.widgetchat.app.ui.ThemePref

/**
 * Drop-in Widget-Chat AI chat, as a Jetpack Compose component.
 *
 * ```kotlin
 * WidgetChat(secretKey = "YOUR_PROJECT_KEY", userId = user.id)
 * ```
 *
 * Configure the bot (name, avatar, theme, languages, tools) from the no-code dashboard
 * at https://widget-chat.com — this renders it natively with Jetpack Compose.
 *
 * @param secretKey Your public project secret key from the dashboard.
 * @param userId A stable identifier for the end user (drives history & identity).
 * @param baseUrl API base URL; defaults to the Widget-Chat cloud.
 * @param isReadOnly When true, disables the composer.
 */
@Composable
fun WidgetChat(
    secretKey: String,
    userId: String,
    baseUrl: String = "https://api.widget-chat.com",
    isReadOnly: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val vm: ChatViewModel = viewModel(
        key = "widgetchat_$secretKey$userId",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(ChatOptions(secretKey = secretKey, userId = userId, baseUrl = baseUrl, isReadOnly = isReadOnly)) as T
        }
    )

    LaunchedEffect(Unit) { vm.start() }

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@rememberLauncherForActivityResult
        vm.send("", imageB64 = Base64.encodeToString(bytes, Base64.NO_WRAP))
    }

    val state by vm.state.collectAsState()
    val dark = when (state.themePref) {
        ThemePref.DARK -> true
        ThemePref.LIGHT -> false
        ThemePref.SYSTEM -> isSystemInDarkTheme()
    }

    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        Surface(modifier = modifier) {
            ChatScreen(
                vm = vm,
                onOpenUrl = { url -> runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } },
                onPickImage = { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            )
        }
    }
}
