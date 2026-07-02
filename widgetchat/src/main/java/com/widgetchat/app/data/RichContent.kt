package com.widgetchat.app.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// MARK: - Domain types (parsed manually for graceful handling of unknown variants)

data class RichContentResponse(
    val components: List<RichComponent> = emptyList(),
    val fallbackText: String? = null,
    val actions: List<RichAction> = emptyList(),
)

sealed interface RichComponent {
    val id: String

    data class TextComp(override val id: String, val content: String, val format: String) : RichComponent
    data class CardComp(val card: RichCard) : RichComponent { override val id get() = card.id }
    data class Carousel(override val id: String, val items: List<RichComponent>) : RichComponent
    data class ProductList(override val id: String, val items: List<RichComponent>, val layout: String?, val columns: Int?) : RichComponent
    data class ImageComp(val image: RichImage) : RichComponent { override val id get() = image.id }
    data class ButtonGroup(override val id: String, val buttons: List<RichButton>, val layout: String) : RichComponent
    data class SwatchGrid(override val id: String, val swatches: List<RichSwatch>, val columns: Int) : RichComponent
    data class HostSlot(override val id: String, val slotId: String) : RichComponent
    data class Unknown(override val id: String) : RichComponent
}

data class RichCard(
    val id: String,
    val title: String?,
    val image: RichImage?,
    val subtitle: String?,
    val description: String?,
    val price: String?,
    val actions: List<RichAction>,
)

data class RichImage(
    val id: String,
    val url: String,
    val alt: String?,
    val caption: String?,
    val aspectRatio: String?,
    val actions: List<RichAction>,
) {
    val ratio: Float
        get() {
            val parts = aspectRatio?.split(":") ?: return 1f
            if (parts.size != 2) return 1f
            val w = parts[0].toFloatOrNull() ?: return 1f
            val h = parts[1].toFloatOrNull() ?: return 1f
            return if (h == 0f) 1f else w / h
        }
}

data class RichButton(val label: String, val action: RichAction?, val style: String)
data class RichSwatch(val hex: String, val label: String?, val action: RichAction?)

sealed interface RichAction {
    val label: String?
    val style: String

    data class OpenUrl(val url: String, override val label: String?, val openInApp: Boolean, override val style: String) : RichAction
    data class SendMessage(val message: String, override val label: String?, val showInChat: Boolean, override val style: String) : RichAction
    data class Callback(val callbackId: String, override val label: String?, override val style: String) : RichAction
    data class RequestInput(val inputType: String, override val label: String?, val source: String, override val style: String) : RichAction
    data class ChangeLanguage(val locale: String) : RichAction { override val label: String? get() = null; override val style get() = "primary" }
    data object Unknown : RichAction { override val label: String? get() = null; override val style get() = "primary" }
}

// MARK: - Parsing helpers

private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.let { if (it.isString || it.content != "null") it.content else null }
private fun JsonObject.bool(key: String, default: Boolean): Boolean = (this[key] as? JsonPrimitive)?.booleanOrNull ?: default
private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull

object RichParser {
    fun parseResponse(obj: JsonObject): RichContentResponse {
        val comps = (obj["components"] as? JsonArray)?.mapNotNull { (it as? JsonObject)?.let(::parseComponent) } ?: emptyList()
        val actions = (obj["actions"] as? JsonArray)?.mapNotNull { (it as? JsonObject)?.let(::parseAction) } ?: emptyList()
        return RichContentResponse(comps, obj.str("fallback_text"), actions)
    }

    fun parseComponent(obj: JsonObject): RichComponent {
        val type = obj.str("type") ?: "unknown"
        val id = obj.str("id") ?: type
        fun items() = (obj["items"] as? JsonArray)?.mapNotNull { (it as? JsonObject)?.let(::parseComponent) } ?: emptyList()
        val config = obj["config"] as? JsonObject
        return when (type) {
            "text" -> RichComponent.TextComp(id, obj.str("content") ?: "", obj.str("format") ?: "markdown")
            "card" -> RichComponent.CardComp(parseCard(obj))
            "carousel" -> RichComponent.Carousel(id, items())
            "product_list" -> RichComponent.ProductList(id, items(), config?.str("layout"), config?.int("columns"))
            "image" -> RichComponent.ImageComp(parseImage(obj))
            "button_group" -> RichComponent.ButtonGroup(id,
                (obj["buttons"] as? JsonArray)?.mapNotNull { (it as? JsonObject)?.let(::parseButton) } ?: emptyList(),
                obj.str("layout") ?: "horizontal")
            "swatch_grid" -> RichComponent.SwatchGrid(id,
                (obj["swatches"] as? JsonArray)?.mapNotNull { (it as? JsonObject)?.let(::parseSwatch) } ?: emptyList(),
                obj.int("columns") ?: 4)
            "host_slot" -> RichComponent.HostSlot(id, obj.str("slot_id") ?: "")
            else -> RichComponent.Unknown(id)
        }
    }

    private fun parseCard(obj: JsonObject): RichCard {
        val meta = obj["metadata"] as? JsonObject
        val price = (meta?.get("price") as? JsonPrimitive)?.content
        return RichCard(
            id = obj.str("id") ?: "card",
            title = obj.str("title"),
            image = (obj["image"] as? JsonObject)?.let(::parseImage),
            subtitle = obj.str("subtitle"),
            description = obj.str("description"),
            price = price,
            actions = (obj["actions"] as? JsonArray)?.mapNotNull { (it as? JsonObject)?.let(::parseAction) } ?: emptyList(),
        )
    }

    private fun parseImage(obj: JsonObject): RichImage = RichImage(
        id = obj.str("id") ?: "image",
        url = obj.str("url") ?: "",
        alt = obj.str("alt"),
        caption = obj.str("caption"),
        aspectRatio = obj.str("aspect_ratio"),
        actions = (obj["actions"] as? JsonArray)?.mapNotNull { (it as? JsonObject)?.let(::parseAction) } ?: emptyList(),
    )

    private fun parseButton(obj: JsonObject): RichButton = RichButton(
        label = obj.str("label") ?: "",
        action = (obj["action"] as? JsonObject)?.let(::parseAction),
        style = obj.str("style") ?: "primary",
    )

    private fun parseSwatch(obj: JsonObject): RichSwatch = RichSwatch(
        hex = obj.str("hex") ?: "#000000",
        label = obj.str("label"),
        action = (obj["action"] as? JsonObject)?.let(::parseAction),
    )

    fun parseAction(obj: JsonObject): RichAction {
        val type = obj.str("type") ?: ""
        val label = obj.str("label")
        val style = obj.str("style") ?: "primary"
        return when (type) {
            "open_url" -> RichAction.OpenUrl(obj.str("url") ?: "", label, obj.bool("open_in_app", false), style)
            "send_message" -> RichAction.SendMessage(obj.str("message") ?: "", label, obj.bool("show_in_chat", true), style)
            "callback" -> RichAction.Callback(obj.str("callback_id") ?: "", label, style)
            "request_input" -> RichAction.RequestInput(obj.str("input_type") ?: "image", label, obj.str("source") ?: "both", style)
            "change_language" -> RichAction.ChangeLanguage(obj.str("locale") ?: "en")
            else -> RichAction.Unknown
        }
    }
}
