package com.widgetchat.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.widgetchat.app.data.ChatMessage
import com.widgetchat.app.data.MessageContent
import com.widgetchat.app.data.MessageStatus
import com.widgetchat.app.ui.theme.ChatColors

@Composable
fun ChatScreen(vm: ChatViewModel, onOpenUrl: (String) -> Unit, onPickImage: () -> Unit) {
    val state by vm.state.collectAsState()
    val dark = when (state.themePref) {
        ThemePref.DARK -> true
        ThemePref.LIGHT -> false
        ThemePref.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val colors = remember(state.config, dark) { ChatColors.resolve(state.config, dark) }

    var reportTarget by remember { mutableStateOf<ChatMessage?>(null) }

    Column(
        Modifier.fillMaxSize().background(colors.surface).statusBarsPadding()
    ) {
        ChatHeader(vm, state, colors, dark)

        if (state.isLoading) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) { CircularProgressIndicator() }
        } else {
            MessageList(vm, state, colors, Modifier.weight(1f), onOpenUrl) { msg -> reportTarget = msg }
        }

        if (!state.chatLocked) {
            InputBar(state, colors, vm.isReadOnly, onPickImage) { text -> vm.send(text) }
        }
        Text(
            Strings.get(Strings.Key.PoweredBy, state.locale),
            color = Color.Gray, fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp).navigationBarsPadding(),
            textAlign = TextAlign.Center,
        )
    }

    reportTarget?.let { target ->
        ReportSheet(state.locale, colors, onDismiss = { reportTarget = null }) { reason, note ->
            vm.rate(target, "bad", reason, note)
            reportTarget = null
        }
    }
}

@Composable
private fun ChatHeader(vm: ChatViewModel, state: ChatUiState, colors: ChatColors, dark: Boolean) {
    var menu by remember { mutableStateOf(false) }
    var langMenu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BotAvatar(state.config?.avatar, colors, 34)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(vm.botName(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF34C759)))
                Spacer(Modifier.width(4.dp))
                Text(Strings.get(Strings.Key.Online, state.locale), fontSize = 11.sp, color = Color.Gray)
            }
        }
        if (state.config?.showThemeToggle != false) {
            IconButton(onClick = { vm.setTheme(if (dark) ThemePref.LIGHT else ThemePref.DARK) }) {
                Icon(if (dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode, "Theme", tint = colors.primary)
            }
        }
        val locales = state.config?.supportedLocales ?: emptyList()
        if (locales.size > 1) {
            Box {
                IconButton(onClick = { langMenu = true }) { Icon(Icons.Outlined.Language, "Language", tint = colors.primary) }
                DropdownMenu(langMenu, { langMenu = false }) {
                    locales.forEach { loc ->
                        DropdownMenuItem(text = { Text(loc.uppercase()) },
                            onClick = { vm.changeLanguage(loc); langMenu = false })
                    }
                }
            }
        }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "Menu", tint = colors.primary) }
            DropdownMenu(menu, { menu = false }) {
                if (state.config?.enableClearHistory != false) {
                    DropdownMenuItem(text = { Text(Strings.get(Strings.Key.ClearHistory, state.locale)) },
                        onClick = { vm.clearHistory(); menu = false })
                }
            }
        }
    }
}

@Composable
private fun MessageList(
    vm: ChatViewModel, state: ChatUiState, colors: ChatColors, modifier: Modifier,
    onOpenUrl: (String) -> Unit, onReport: (ChatMessage) -> Unit,
) {
    val listState = rememberLazyListState()
    // Auto-scroll to the NEWEST message — keyed on its id, not the total count,
    // so prepending an older page (which leaves the last id unchanged) never
    // yanks the view to the bottom.
    androidx.compose.runtime.LaunchedEffect(state.messages.lastOrNull()?.id, state.isBotTyping) {
        val count = state.messages.size + if (state.isBotTyping) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }
    // Scroll-back: fetch the next older page as the visitor nears the top, and
    // remember the current top message so we can re-anchor after the prepend.
    var anchorKey by remember { mutableStateOf<String?>(null) }
    var anchorOffset by remember { mutableStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(listState) {
        androidx.compose.runtime.snapshotFlow { listState.firstVisibleItemIndex }
            .collect { idx ->
                if (idx <= 1 && state.hasMore && !state.isLoadingOlder) {
                    anchorKey = state.messages.getOrNull(idx)?.id
                    anchorOffset = listState.firstVisibleItemScrollOffset
                    vm.loadOlderMessages()
                }
            }
    }
    // After an older page lands (the first message id changes), pin the
    // previously-top message back under the viewport so content doesn't jump.
    androidx.compose.runtime.LaunchedEffect(state.messages.firstOrNull()?.id) {
        val key = anchorKey ?: return@LaunchedEffect
        val newIdx = state.messages.indexOfFirst { it.id == key }
        if (newIdx > 0) {
            listState.scrollToItem(newIdx, anchorOffset)
            anchorKey = null
        }
    }
    val density = androidx.compose.ui.platform.LocalDensity.current
    var bannerHeight by remember { mutableStateOf(0.dp) }
    val topPad = if (state.showDisclosure) bannerHeight + 12.dp else 12.dp
    Box(modifier.fillMaxWidth()) {
        LazyColumn(Modifier.fillMaxSize(), state = listState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 12.dp, end = 12.dp, top = topPad, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.messages.size, key = { state.messages[it].id }) { i ->
                val msg = state.messages[i]
                MessageBubble(msg, colors, state, vm,
                    onAction = { vm.handleAction(it, onOpenUrl) },
                    onRate = { rating -> if (rating == "good") vm.rate(msg, "good") else onReport(msg) })
            }
            if (state.isBotTyping) item { TypingRow(colors) }
        }
        // Small spinner overlaid at the top while an older page loads. Kept as
        // an overlay (not a list item) so list indices stay 1:1 with messages
        // and the re-anchor math above stays simple.
        if (state.isLoadingOlder) {
            CircularProgressIndicator(
                Modifier.align(Alignment.TopCenter).padding(top = topPad + 4.dp).size(20.dp),
                strokeWidth = 2.dp,
            )
        }
        // Disclaimer is a floating card pinned at the top; messages scroll
        // underneath it (hidden behind) rather than pushing it away. Its measured
        // height is reserved as list top padding so content clears it at rest.
        if (state.showDisclosure) {
            Box(Modifier.align(Alignment.TopCenter).padding(12.dp)
                .onSizeChanged { bannerHeight = with(density) { it.height.toDp() } }) {
                DisclosureBanner(vm, state, colors)
            }
        }
    }
}

@Composable
private fun MessageBubble(
    msg: ChatMessage, colors: ChatColors, state: ChatUiState, vm: ChatViewModel,
    onAction: (com.widgetchat.app.data.RichAction) -> Unit, onRate: (String) -> Unit,
) {
    if (msg.isSystem) {
        Box(Modifier.fillMaxWidth(), Alignment.Center) {
            Text(msg.content.plainText, color = Color.Gray, fontSize = 13.sp,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.Gray.copy(alpha = 0.12f))
                    .padding(horizontal = 14.dp, vertical = 8.dp))
        }
        return
    }
    val bubbleColor = if (msg.isUser) colors.userBubble else colors.botBubble
    val textColor = if (msg.isUser) colors.userText else colors.botText
    // A single weighted spacer absorbs all slack and pushes the (non-weighted, width-capped)
    // bubble column fully to its side. BoxWithConstraints gives a finite cap so text still wraps.
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
      val maxBubble = maxWidth * 0.82f
      // Rich content (carousels/cards) uses the full available width; text/user bubbles hug content.
      val isRich = msg.content is MessageContent.Rich && !msg.isUser
      Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        if (msg.isUser) Spacer(Modifier.weight(1f))
        if (!msg.isUser && state.config?.showAvatar != false) {
            BotAvatar(state.config?.avatar, colors, 28); Spacer(Modifier.width(8.dp))
        }
        Column(
            horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start,
            modifier = if (isRich) Modifier.weight(1f) else Modifier.widthIn(max = maxBubble),
        ) {
            Column(
                Modifier.clip(bubbleShape(msg.isUser)).background(bubbleColor)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                msg.imageB64?.let { b64 ->
                    val bytes = remember(b64) { runCatching { android.util.Base64.decode(b64, android.util.Base64.DEFAULT) }.getOrNull() }
                    bytes?.let {
                        AsyncImage(model = it, contentDescription = null,
                            modifier = Modifier.size(180.dp).clip(RoundedCornerShape(12.dp)))
                    }
                }
                when (val c = msg.content) {
                    is MessageContent.Rich -> RichContentView(c.response, colors, onAction)
                    is MessageContent.Markdown -> MarkdownText(c.text, textColor)
                    is MessageContent.Html -> MarkdownText(c.text, textColor)
                    is MessageContent.Code -> Text(c.text, color = textColor, fontSize = 13.sp)
                    is MessageContent.Text -> Text(c.text, color = textColor)
                }
                if (msg.isBackendPending) Text(Strings.get(Strings.Key.Working, state.locale), color = Color.Gray, fontSize = 12.sp)
            }
            if (!msg.isUser && state.config?.messageReportingEnabled != false && !msg.isBackendPending) {
                RatingRow(msg, onRate)
            }
            if (msg.isUser) {
                when (msg.status) {
                    MessageStatus.FAILED -> Text("Failed", color = Color(0xFFE53935), fontSize = 11.sp)
                    MessageStatus.SENDING -> Text("Sending…", color = Color.Gray, fontSize = 10.sp)
                    else -> Text("Sent ✓", color = Color.Gray, fontSize = 10.sp)
                }
            }
        }
        if (!msg.isUser && !isRich) Spacer(Modifier.weight(1f))
      }
    }
}

@Composable
private fun RatingRow(msg: ChatMessage, onRate: (String) -> Unit) {
    if (msg.rating != null) {
        Text(if (msg.rating == "good") "Thanks for the feedback" else "Feedback noted",
            color = Color.Gray, fontSize = 11.sp)
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 2.dp)) {
            Icon(Icons.Outlined.ThumbUp, "Good", tint = Color.Gray,
                modifier = Modifier.size(16.dp).clickable { onRate("good") })
            Icon(Icons.Outlined.ThumbDown, "Bad", tint = Color.Gray,
                modifier = Modifier.size(16.dp).clickable { onRate("bad") })
        }
    }
}

@Composable
private fun TypingRow(colors: ChatColors) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(Modifier.clip(RoundedCornerShape(18.dp)).background(colors.botBubble).padding(16.dp)) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = colors.botText.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun DisclosureBanner(vm: ChatViewModel, state: ChatUiState, colors: ChatColors) {
    val cfg = state.config
    Column(
        // Opaque floating card pinned over the scrolling messages: solid surface + shadow.
        Modifier.fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, colors.primary.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(cfg?.aiDisclosureText?.resolved(state.locale) ?: Strings.get(Strings.Key.AiDisclosure, state.locale),
            color = Color.Gray, fontSize = 13.sp)
        if (cfg?.aiDataReuseNotice == true) {
            Text(Strings.get(Strings.Key.DataReuseNotice, state.locale), color = Color.Gray, fontSize = 11.sp)
        }
        if (cfg?.aiDisclosureRequireAck == true) {
            ActionButton(Strings.get(Strings.Key.GotIt, state.locale), "primary", colors) { vm.acknowledgeDisclosure() }
        }
    }
}

@Composable
private fun InputBar(state: ChatUiState, colors: ChatColors, readOnly: Boolean, onPickImage: () -> Unit, onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val canSend = !readOnly && text.isNotBlank()
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).imePadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.config?.enableFileAttach != false && !readOnly) {
            IconButton(onClick = onPickImage) { Icon(Icons.Outlined.AttachFile, "Attach", tint = colors.primary) }
        }
        TextField(
            value = text, onValueChange = { text = it },
            placeholder = { Text(Strings.get(Strings.Key.InputPlaceholder, state.locale)) },
            enabled = !readOnly,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            maxLines = 5,
            keyboardOptions = KeyboardOptions.Default,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = { if (canSend) { onSend(text); text = "" } }, enabled = canSend) {
            Icon(Icons.Filled.Send, Strings.get(Strings.Key.Send, state.locale),
                tint = if (canSend) colors.primary else Color.Gray)
        }
    }
}

@Composable
fun BotAvatar(avatar: String?, colors: ChatColors, size: Int) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(colors.primary.copy(alpha = 0.15f)), Alignment.Center) {
        if (avatar != null && avatar.startsWith("http")) {
            AsyncImage(model = avatar, contentDescription = "avatar", modifier = Modifier.fillMaxSize())
        } else if (avatar != null && (avatar.startsWith("data:") || avatar.length > 100)) {
            val b64 = if (avatar.startsWith("data:")) avatar.substringAfter(",", "") else avatar
            val bytes = remember(b64) { runCatching { android.util.Base64.decode(b64, android.util.Base64.DEFAULT) }.getOrNull() }
            if (bytes != null) AsyncImage(model = bytes, contentDescription = "avatar", modifier = Modifier.fillMaxSize())
            else Text("AI", color = colors.primary, fontSize = (size / 2.6).sp, fontWeight = FontWeight.Bold)
        } else {
            Text("AI", color = colors.primary, fontSize = (size / 2.6).sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun bubbleShape(isUser: Boolean) = RoundedCornerShape(
    topStart = 18.dp, topEnd = 18.dp,
    bottomStart = if (isUser) 18.dp else 5.dp,
    bottomEnd = if (isUser) 5.dp else 18.dp,
)
