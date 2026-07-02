package com.widgetchat.app.network

import com.widgetchat.app.data.AppJson
import com.widgetchat.app.data.ChatMessage
import com.widgetchat.app.data.MessageParser
import com.widgetchat.app.data.RemoteConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class QuotaException(val serverMessage: String?) : Exception(serverMessage)

/** OkHttp client for the Widget-Chat backend. Mirrors the iOS/Flutter clients. */
class ApiClient(
    private val baseUrl: String = "https://api.widget-chat.com",
    private val secretKey: String,
    private val userIdSignature: String? = null,
) {
    private val json = "application/json".toMediaType()
    private val http = OkHttpClient.Builder()
        .callTimeout(180, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    suspend fun fetchConfig(): RemoteConfig = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$baseUrl/api/project/config/")
            .header("X-Project-Secret-Key", secretKey)
            .build()
        http.newCall(req).execute().use { resp ->
            val body = resp.body?.string() ?: "{}"
            AppJson.decodeFromString(RemoteConfig.serializer(), body)
        }
    }

    suspend fun sendMessage(
        message: String, userId: String, locale: String,
        imageB64: String? = null, imageMime: String? = null,
        userAuthToken: String? = null,
    ): ChatMessage = withContext(Dispatchers.IO) {
        val payload = buildJsonObject {
            put("message", message)
            put("user_id", userId)
            put("secret_key", secretKey)
            put("locale", locale)
            put("platform", "android")
            imageB64?.let { put("image_b64", it) }
            imageMime?.let { put("image_mime_type", it) }
            userAuthToken?.takeIf { it.isNotEmpty() }?.let { put("user_auth_token", it) }
            userIdSignature?.takeIf { it.isNotEmpty() }?.let { put("user_id_signature", it) }
        }
        val req = Request.Builder()
            .url("$baseUrl/api/chat/")
            .post(payload.toString().toRequestBody(json))
            .build()
        http.newCall(req).execute().use { resp ->
            val body = resp.body?.string() ?: "{}"
            if (resp.code == 429) {
                val msg = (AppJson.parseToJsonElement(body).jsonObject["message"] as? JsonPrimitive)?.content
                throw QuotaException(msg)
            }
            MessageParser.fromServer(AppJson.parseToJsonElement(body).jsonObject)
        }
    }

    suspend fun history(userId: String): List<ChatMessage> = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$baseUrl/api/chat/history/$userId/")
            .header("X-Project-Secret-Key", secretKey)
            .apply { userIdSignature?.let { header("X-User-Id-Signature", it) } }
            .build()
        http.newCall(req).execute().use { resp ->
            val body = resp.body?.string() ?: "[]"
            (AppJson.parseToJsonElement(body) as? JsonArray)
                ?.mapNotNull { (it as? JsonObject)?.let(MessageParser::fromServer) } ?: emptyList()
        }
    }

    suspend fun clearHistory(userId: String) = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$baseUrl/api/chat/history/$userId/")
            .delete()
            .header("X-Project-Secret-Key", secretKey)
            .apply { userIdSignature?.let { header("X-User-Id-Signature", it) } }
            .build()
        runCatching { http.newCall(req).execute().use { } }
        Unit
    }

    suspend fun reportMessage(
        rating: String, reason: String?, note: String?, serverId: Int?,
        excerpt: String, userId: String, locale: String,
    ) = withContext(Dispatchers.IO) {
        val payload = buildJsonObject {
            put("rating", rating)
            reason?.let { put("reason", it) }
            note?.let { put("note", it) }
            serverId?.let { put("message_server_id", it) }
            put("message_excerpt", excerpt)
            put("user_id_hash", hash(userId))
            put("user_id", userId)
            put("locale", locale)
            put("platform", "android")
        }
        fireAndForget("$baseUrl/api/project/message-reports/", payload)
    }

    suspend fun consentEvent(eventType: String, userId: String, locale: String) = withContext(Dispatchers.IO) {
        val payload = buildJsonObject {
            put("event_type", eventType)
            put("user_id_hash", hash(userId))
            put("locale", locale)
            put("platform", "android")
            put("disclosure_version", "2")
        }
        fireAndForget("$baseUrl/api/project/consent-events/", payload)
    }

    private fun fireAndForget(url: String, payload: JsonObject) {
        val req = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody(json))
            .header("X-Project-Secret-Key", secretKey)
            .build()
        runCatching { http.newCall(req).execute().use { } }
    }

    private fun hash(userId: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(userId.toByteArray())
        return "sha256:" + digest.joinToString("") { "%02x".format(it) }
    }
}
