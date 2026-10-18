package com.vxai.chat.data.model

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.annotations.SerializedName

data class ChatMessage(
    val role: String,
    val content: JsonElement
) {
    companion object {
        fun text(role: String, text: String): ChatMessage =
            ChatMessage(role, JsonPrimitive(text))

        fun withImage(role: String, text: String, dataUrl: String): ChatMessage {
            val arr = com.google.gson.JsonArray()
            if (text.isNotBlank()) {
                val t = com.google.gson.JsonObject()
                t.addProperty("type", "text")
                t.addProperty("text", text)
                arr.add(t)
            }
            val img = com.google.gson.JsonObject()
            img.addProperty("type", "image_url")
            val urlObj = com.google.gson.JsonObject()
            urlObj.addProperty("url", dataUrl)
            img.add("image_url", urlObj)
            arr.add(img)
            return ChatMessage(role, arr)
        }
    }
}

fun ChatMessage.displayText(): String {
    return when {
        content.isJsonPrimitive && content.asJsonPrimitive.isString -> content.asString
        content.isJsonArray -> content.asJsonArray
            .mapNotNull { el ->
                val o = el.asJsonObject
                if (o.get("type")?.asString == "text") o.get("text")?.asString else null
            }
            .joinToString("\n")
            .ifBlank { "[image]" }
        else -> content.toString()
    }
}

fun ChatMessage.hasImage(): Boolean =
    content.isJsonArray && content.asJsonArray.any {
        it.asJsonObject.get("type")?.asString == "image_url"
    }

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = false
)

data class ChatChoice(
    val message: ChatMessage,
    @SerializedName("finish_reason") val finishReason: String? = null
)

data class ChatResponse(
    val id: String? = null,
    val choices: List<ChatChoice> = emptyList()
)

data class ModelInfo(val id: String)

data class ModelList(val data: List<ModelInfo> = emptyList())
