package com.widgetchat.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.widgetchat.app.data.ChatAppearance
import com.widgetchat.app.data.RemoteConfig

/** Parse #RGB / #RRGGBB / #AARRGGBB into a Compose Color, or null. */
fun parseHex(hex: String?): Color? {
    var raw = hex?.trim()?.removePrefix("#") ?: return null
    if (raw.isEmpty()) return null
    return try {
        when (raw.length) {
            3 -> {
                val r = raw[0].digitToInt(16) * 17
                val g = raw[1].digitToInt(16) * 17
                val b = raw[2].digitToInt(16) * 17
                Color(r, g, b)
            }
            6 -> {
                val v = raw.toLong(16)
                Color((v shr 16 and 0xFF).toInt(), (v shr 8 and 0xFF).toInt(), (v and 0xFF).toInt())
            }
            8 -> {
                val v = raw.toLong(16)
                Color(
                    (v shr 16 and 0xFF).toInt(), (v shr 8 and 0xFF).toInt(),
                    (v and 0xFF).toInt(), (v shr 24 and 0xFF).toInt()
                )
            }
            else -> null
        }
    } catch (_: Exception) { null }
}

fun Color.readableForeground(): Color {
    val luminance = 0.299f * red + 0.587f * green + 0.114f * blue
    return if (luminance > 0.6f) Color.Black else Color.White
}

data class ChatColors(
    val primary: Color,
    val surface: Color,
    val botBubble: Color,
    val userBubble: Color,
    val botText: Color,
    val userText: Color,
) {
    fun actionColors(style: String): Pair<Color, Color> = when (style) {
        "primary" -> primary to primary.readableForeground()
        "danger" -> Color(0xFFE53935) to Color.White
        else -> primary.copy(alpha = 0.12f) to primary
    }

    companion object {
        fun resolve(config: RemoteConfig?, dark: Boolean): ChatColors {
            val appearance: ChatAppearance? =
                if (dark) config?.chatAppearanceDark ?: config?.chatAppearance
                else config?.chatAppearance
            val primary = parseHex(config?.color) ?: Color(0xFF2F6FED)
            val surface = parseHex(appearance?.surfaceColor)
                ?: if (dark) Color(0xFF121212) else Color(0xFFF7F7F8)
            val botBubble = parseHex(appearance?.botBubbleColor)
                ?: if (dark) Color(0xFF2A2A2E) else Color.White
            val userBubble = parseHex(appearance?.userBubbleColor) ?: primary
            val botText = parseHex(appearance?.botTextColor)
                ?: if (dark) Color.White else Color(0xFF111111)
            val userText = parseHex(appearance?.userTextColor) ?: userBubble.readableForeground()
            return ChatColors(primary, surface, botBubble, userBubble, botText, userText)
        }
    }
}
