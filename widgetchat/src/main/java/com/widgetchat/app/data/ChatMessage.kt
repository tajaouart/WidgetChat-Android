package com.widgetchat.app.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID

enum class MessageStatus { SENDING, SENT, DELIVERED, FAILED, READ }

sealed interface MessageContent {
    val plainText: String

    data class Text(val text: String) : MessageContent { override val plainText get() = text }
    data class Markdown(val text: String) : MessageContent { override val plainText get() = text }
    data class Code(val text: String) : MessageContent { override val plainText get() = text }
    data class Html(val text: String) : MessageContent { override val plainText get() = text }
    data class Rich(val response: RichContentResponse) : MessageContent {
        override val plainText get() = response.fallbackText ?: "[interactive content]"
    }
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val serverId: Int? = null,
    val content: MessageContent,
    val isUser: Boolean,
    val isSystem: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENT,
    val imageUrl: String? = null,
    val imageB64: String? = null,
    val imageMimeType: String? = null,
    val isBackendPending: Boolean = false,
    val rating: String? = null,
)

/** Parses the raw server envelope + its nested `{"response_type","data"}` message string. */
object MessageParser {
    fun fromServer(obj: JsonObject): ChatMessage {
        fun s(k: String) = (obj[k] as? JsonPrimitive)?.let { if (it.content == "null") null else it.content }
        val id = (obj["id"] as? JsonPrimitive)?.content?.toIntOrNull()
        val sender = s("sender") ?: "bot"
        val pending = (obj["is_pending"] as? JsonPrimitive)?.content?.toBoolean() ?: false
        return ChatMessage(
            id = id?.toString() ?: UUID.randomUUID().toString(),
            serverId = id,
            content = parseContent(s("message") ?: ""),
            isUser = sender.lowercase() == "user",
            status = MessageStatus.DELIVERED,
            imageUrl = s("image_url"),
            imageB64 = s("image_b64"),
            imageMimeType = s("image_mime_type"),
            isBackendPending = pending,
            rating = s("my_rating"),
        )
    }

    fun parseContent(raw: String): MessageContent {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("{")) return MessageContent.Text(raw)
        val obj = runCatching { AppJson.parseToJsonElement(trimmed) as? JsonObject }.getOrNull()
            ?: return MessageContent.Text(raw)
        val type = (obj["response_type"] as? JsonPrimitive)?.content ?: return MessageContent.Text(raw)
        val data = obj["data"]
        return when (type) {
            "rich_content" -> (data as? JsonObject)?.let { MessageContent.Rich(RichParser.parseResponse(it)) }
                ?: MessageContent.Text(raw)
            "markdown" -> MessageContent.Markdown((data as? JsonPrimitive)?.content ?: raw)
            "code" -> MessageContent.Code((data as? JsonPrimitive)?.content ?: raw)
            "html" -> MessageContent.Html((data as? JsonPrimitive)?.content ?: raw)
            else -> MessageContent.Text((data as? JsonPrimitive)?.content ?: raw)
        }
    }
}
