package com.widgetchat.app.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

val AppJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    explicitNulls = false
}

/** Localized string. The backend may send a bare String OR an object with `defaultValue`. */
@Serializable(with = LocalizedTextSerializer::class)
data class LocalizedText(
    val defaultValue: String = "",
    val translations: Map<String, String> = emptyMap(),
    val useDefaultOnly: Boolean = false,
) {
    fun resolved(locale: String?): String {
        if (locale == null || useDefaultOnly) return defaultValue
        translations[locale]?.let { return it }
        val lang = locale.take(2)
        translations[lang]?.let { return it }
        translations.entries.firstOrNull { it.key.startsWith(lang) }?.let { return it.value }
        return defaultValue
    }
}

object LocalizedTextSerializer : KSerializer<LocalizedText> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalizedText", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalizedText {
        val input = decoder as? JsonDecoder ?: return LocalizedText()
        val element = input.decodeJsonElement()
        if (element is JsonPrimitive && element.isString) {
            return LocalizedText(defaultValue = element.content)
        }
        val obj = element as? JsonObject ?: return LocalizedText()
        val default = (obj["defaultValue"] as? JsonPrimitive)?.content ?: ""
        val translations = (obj["translations"] as? JsonObject)?.mapValues {
            (it.value as? JsonPrimitive)?.content ?: ""
        } ?: emptyMap()
        val useDefault = (obj["useDefaultOnly"] as? JsonPrimitive)?.content?.toBoolean() ?: false
        return LocalizedText(default, translations, useDefault)
    }

    override fun serialize(encoder: Encoder, value: LocalizedText) {
        encoder.encodeString(value.defaultValue)
    }
}

@Serializable
data class RemoteConfig(
    val name: LocalizedText? = null,
    @SerialName("welcome_message") val welcomeMessage: LocalizedText? = null,
    @SerialName("system_instruction") val systemInstruction: LocalizedText? = null,
    @SerialName("default_locale") val defaultLocale: String = "en",
    @SerialName("supported_locales") val supportedLocales: List<String> = listOf("en"),
    @SerialName("font_family") val fontFamily: String? = null,
    val color: String? = null,
    val avatar: String? = null,
    @SerialName("show_avatar") val showAvatar: Boolean = true,
    @SerialName("fab_configuration") val fabConfiguration: FabConfiguration? = null,
    @SerialName("chat_appearance") val chatAppearance: ChatAppearance? = null,
    @SerialName("chat_appearance_dark") val chatAppearanceDark: ChatAppearance? = null,
    @SerialName("sound_configuration") val soundConfiguration: SoundConfiguration? = null,
    @SerialName("enable_user_export") val enableUserExport: Boolean = true,
    @SerialName("enable_clear_history") val enableClearHistory: Boolean = true,
    @SerialName("enable_file_attach") val enableFileAttach: Boolean = true,
    @SerialName("dynamic_language_enabled") val dynamicLanguageEnabled: Boolean = false,
    @SerialName("ai_disclosure_enabled") val aiDisclosureEnabled: Boolean = true,
    @SerialName("ai_disclosure_text") val aiDisclosureText: LocalizedText? = null,
    @SerialName("ai_disclosure_require_ack") val aiDisclosureRequireAck: Boolean = false,
    @SerialName("ai_info_url") val aiInfoUrl: String? = null,
    @SerialName("privacy_url") val privacyUrl: String? = null,
    @SerialName("ai_data_reuse_notice") val aiDataReuseNotice: Boolean = false,
    @SerialName("message_reporting_enabled") val messageReportingEnabled: Boolean = true,
    @SerialName("show_theme_toggle") val showThemeToggle: Boolean = true,
    @SerialName("default_theme_mode") val defaultThemeMode: String = "system",
)

@Serializable
data class FabConfiguration(
    val icon: String? = null,
    val iconSize: Double? = null,
    val iconColor: String? = null,
    val backgroundColor: String? = null,
    val buttonSize: Double? = null,
    val borderRadius: Double? = null,
    val useAvatarAsIcon: Boolean? = null,
)

@Serializable
data class ChatAppearance(
    val surfaceColor: String? = null,
    val botBubbleColor: String? = null,
    val userBubbleColor: String? = null,
    val botTextColor: String? = null,
    val userTextColor: String? = null,
)

@Serializable
data class SoundConfiguration(
    val enabled: Boolean = true,
    val volume: Double = 0.5,
    val messageSent: Boolean = true,
    val messageReceived: Boolean = true,
    val chatOpened: Boolean = false,
    val chatClosed: Boolean = false,
    val error: Boolean = true,
    val buttonClick: Boolean = false,
    val notification: Boolean = true,
)
