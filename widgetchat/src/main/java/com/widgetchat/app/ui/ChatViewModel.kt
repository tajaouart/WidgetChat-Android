package com.widgetchat.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.widgetchat.app.data.ChatMessage
import com.widgetchat.app.data.MessageContent
import com.widgetchat.app.data.MessageStatus
import com.widgetchat.app.data.RemoteConfig
import com.widgetchat.app.data.RichAction
import com.widgetchat.app.network.ApiClient
import com.widgetchat.app.network.QuotaException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class ChatOptions(
    val secretKey: String,
    val userId: String,
    val baseUrl: String = "https://api.widget-chat.com",
    val isReadOnly: Boolean = false,
)

enum class ThemePref { LIGHT, DARK, SYSTEM }

data class ChatUiState(
    val config: RemoteConfig? = null,
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = true,
    val isBotTyping: Boolean = false,
    val chatLocked: Boolean = false,
    val showDisclosure: Boolean = false,
    val locale: String = "en",
    val themePref: ThemePref = ThemePref.SYSTEM,
    val error: String? = null,
)

class ChatViewModel(private val options: ChatOptions) : ViewModel() {
    private val api = ApiClient(options.baseUrl, options.secretKey)
    private val _state = MutableStateFlow(ChatUiState(locale = deviceLocale()))
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    val isReadOnly get() = options.isReadOnly

    private fun deviceLocale() = Locale.getDefault().language.take(2)

    fun start() {
        viewModelScope.launch {
            try {
                val cfg = api.fetchConfig()
                var locale = _state.value.locale
                if (cfg.supportedLocales.none { it.startsWith(locale.take(2)) }) locale = cfg.defaultLocale
                val theme = when (cfg.defaultThemeMode) {
                    "light" -> ThemePref.LIGHT; "dark" -> ThemePref.DARK; else -> ThemePref.SYSTEM
                }
                _state.value = _state.value.copy(config = cfg, locale = locale, themePref = theme)
                loadHistory(cfg)
                if (cfg.aiDisclosureEnabled) {
                    _state.value = _state.value.copy(showDisclosure = true)
                    api.consentEvent("disclosure_shown", options.userId, locale)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = Strings.get(Strings.Key.ChatUnavailable, _state.value.locale))
            }
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    private suspend fun loadHistory(cfg: RemoteConfig) {
        val history = runCatching { api.history(options.userId) }.getOrDefault(emptyList())
        if (history.isEmpty()) {
            val welcome = cfg.welcomeMessage?.resolved(_state.value.locale)
            if (!welcome.isNullOrEmpty()) {
                _state.value = _state.value.copy(
                    messages = listOf(ChatMessage(content = MessageContent.Markdown(welcome), isUser = false))
                )
            }
        } else {
            _state.value = _state.value.copy(messages = history)
            pollPendingIfNeeded()
        }
    }

    fun acknowledgeDisclosure() {
        _state.value = _state.value.copy(showDisclosure = false)
        viewModelScope.launch { api.consentEvent("consent_given", options.userId, _state.value.locale) }
    }

    fun send(text: String, imageB64: String? = null, showUserBubble: Boolean = true) {
        val trimmed = text.trim()
        val s = _state.value
        if (s.chatLocked || options.isReadOnly) return
        if (trimmed.isEmpty() && imageB64 == null) return

        val list = s.messages.toMutableList()
        if (showUserBubble) {
            list.add(ChatMessage(content = MessageContent.Text(trimmed), isUser = true,
                status = MessageStatus.SENDING, imageB64 = imageB64,
                imageMimeType = imageB64?.let { "image/jpeg" }))
        }
        _state.value = s.copy(messages = list, isBotTyping = true)
        SoundPlayer.play("sent", s.config?.soundConfiguration)

        viewModelScope.launch {
            try {
                val reply = api.sendMessage(trimmed, options.userId, _state.value.locale,
                    imageB64 = imageB64, imageMime = imageB64?.let { "image/jpeg" })
                val updated = _state.value.messages.map {
                    if (it.isUser && it.status == MessageStatus.SENDING) it.copy(status = MessageStatus.DELIVERED) else it
                }.toMutableList()
                updated.add(reply)
                _state.value = _state.value.copy(messages = updated, isBotTyping = false)
                SoundPlayer.play("received", _state.value.config?.soundConfiguration)
                handleSilentActions(reply)
                if (reply.isBackendPending) pollPendingIfNeeded()
            } catch (e: QuotaException) {
                val updated = _state.value.messages.toMutableList()
                updated.add(ChatMessage(
                    content = MessageContent.Text(e.serverMessage ?: Strings.get(Strings.Key.ChatUnavailable, _state.value.locale)),
                    isUser = false, isSystem = true))
                _state.value = _state.value.copy(messages = updated, isBotTyping = false, chatLocked = true)
            } catch (e: Exception) {
                val updated = _state.value.messages.map {
                    if (it.isUser && it.status == MessageStatus.SENDING) it.copy(status = MessageStatus.FAILED) else it
                }
                SoundPlayer.play("error", _state.value.config?.soundConfiguration)
                _state.value = _state.value.copy(messages = updated, isBotTyping = false,
                    error = Strings.get(Strings.Key.ChatUnavailable, _state.value.locale))
            }
        }
    }

    private fun pollPendingIfNeeded() {
        if (_state.value.messages.none { it.isBackendPending }) return
        viewModelScope.launch {
            repeat(60) {
                delay(2000)
                val fresh = runCatching { api.history(options.userId) }.getOrNull() ?: return@repeat
                _state.value = _state.value.copy(messages = fresh)
                if (fresh.none { it.isBackendPending }) return@launch
            }
        }
    }

    fun handleAction(action: RichAction, openUrl: (String) -> Unit) {
        when (action) {
            is RichAction.SendMessage -> send(action.message, showUserBubble = action.showInChat)
            is RichAction.OpenUrl -> openUrl(action.url)
            is RichAction.ChangeLanguage -> changeLanguage(action.locale)
            else -> {}
        }
    }

    private fun handleSilentActions(message: ChatMessage) {
        (message.content as? MessageContent.Rich)?.response?.actions?.forEach {
            if (it is RichAction.ChangeLanguage) changeLanguage(it.locale)
        }
    }

    fun changeLanguage(locale: String) {
        _state.value = _state.value.copy(locale = locale)
    }

    fun setTheme(pref: ThemePref) {
        _state.value = _state.value.copy(themePref = pref)
    }

    fun clearHistory() {
        viewModelScope.launch {
            api.clearHistory(options.userId)
            val cfg = _state.value.config
            val welcome = cfg?.welcomeMessage?.resolved(_state.value.locale)
            val msgs = if (!welcome.isNullOrEmpty())
                listOf(ChatMessage(content = MessageContent.Markdown(welcome), isUser = false)) else emptyList()
            _state.value = _state.value.copy(messages = msgs, chatLocked = false)
        }
    }

    fun rate(message: ChatMessage, rating: String, reason: String? = null, note: String? = null) {
        val updated = _state.value.messages.map { if (it.id == message.id) it.copy(rating = rating) else it }
        _state.value = _state.value.copy(messages = updated)
        viewModelScope.launch {
            api.reportMessage(rating, reason, note, message.serverId,
                message.content.plainText.take(280), options.userId, _state.value.locale)
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }

    fun botName(): String = _state.value.config?.name?.resolved(_state.value.locale) ?: "Assistant"
}
