package com.vxai.chat.data.repository

import com.google.gson.Gson
import com.vxai.chat.data.api.NineRouterApi
import com.vxai.chat.data.model.ChatMessage
import com.vxai.chat.data.model.ChatRequest
import com.vxai.chat.data.model.ModelInfo
import com.vxai.chat.util.Prefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ChatRepository(private val prefs: Prefs) {

    private var cachedKey: String = ""
    private var api: NineRouterApi? = null

    private fun buildApi(): NineRouterApi {
        val key = prefs.baseUrl + "|" + prefs.apiKey
        if (api != null && key == cachedKey) return api!!
        cachedKey = key
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .apply { if (prefs.apiKey.isNotBlank()) addHeader("Authorization", "Bearer ${prefs.apiKey}") }
                    .build()
                chain.proceed(req)
            }
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(ensureSlash(prefs.baseUrl))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(NineRouterApi::class.java)
        return api!!
    }

    private fun ensureSlash(url: String): String =
        if (url.endsWith("/")) url else "$url/"

    suspend fun fetchModels(): List<ModelInfo> =
        buildApi().models().data

    suspend fun send(messages: List<ChatMessage>, model: String): String {
        val resp = buildApi().chat(com.vxai.chat.data.model.ChatRequest(model = model, messages = messages))
        return resp.choices.firstOrNull()?.message?.displayText()
            ?: " (no response) "
    }

    fun sendStreaming(messages: List<ChatMessage>, model: String): Flow<String> = flow {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .apply { if (prefs.apiKey.isNotBlank()) addHeader("Authorization", "Bearer ${prefs.apiKey}") }
                    .build()
                chain.proceed(req)
            }
            .build()
        val json = Gson().toJson(com.vxai.chat.data.model.ChatRequest(model = model, messages = messages, stream = true))
        val body = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("${ensureSlash(prefs.baseUrl)}chat/completions")
            .post(body)
            .addHeader("Accept", "text/event-stream")
            .build()
        val response = client.newCall(request).execute()
        response.use { resp ->
            val source = resp.body?.source() ?: return@flow
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                val trimmed = line.trim()
                if (!trimmed.startsWith("data:")) continue
                val data = trimmed.removePrefix("data:").trim()
                if (data == "[DONE]") break
                if (data.isEmpty()) continue
                runCatching {
                    val obj = JSONObject(data)
                    val choices = obj.optJSONArray("choices")
                    val delta = choices?.optJSONObject(0)?.optJSONObject("delta")?.optString("content")
                    if (!delta.isNullOrEmpty()) emit(delta)
                }
            }
        }
    }

    fun invalidate() { api = null; cachedKey = "" }
}
