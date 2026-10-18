package com.vxai.chat.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vxai.chat.data.model.ChatMessage
import com.vxai.chat.data.model.ModelInfo
import com.vxai.chat.data.repository.ChatRepository
import com.vxai.chat.util.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(app: Application) : AndroidViewModel(app) {
    val prefs = Prefs(app)
    val repo = ChatRepository(prefs)

    private val gson = Gson()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _models = MutableStateFlow<List<ModelInfo>>(emptyList())
    val models: StateFlow<List<ModelInfo>> = _models.asStateFlow()

    val selectedModel: String get() = prefs.model.ifBlank { _models.value.firstOrNull()?.id ?: "" }

    init {
        prefs.applyTheme()
        loadHistory()
        viewModelScope.launch {
            runCatching { repo.fetchModels() }
                .onSuccess { list ->
                    _models.value = list
                    if (prefs.model.isBlank() && list.isNotEmpty()) prefs.model = list.first().id
                }
                .onFailure { _error.value = "Gagal load model: ${it.message}" }
        }
    }

    fun send(text: String, imageUri: Uri? = null, contentResolver: android.content.ContentResolver? = null) {
        val t = text.trim()
        if (t.isEmpty() && imageUri == null) return
        if (prefs.model.isBlank()) {
            _error.value = "Pilih model dulu di Settings"
            return
        }
        val userMsg = if (imageUri != null && contentResolver != null) {
            val b64 = encodeImage(contentResolver, imageUri)
            if (b64 == null) {
                _error.value = "Gagal baca gambar"
                return
            }
            ChatMessage.withImage("user", t, b64)
        } else {
            ChatMessage.text("user", t)
        }
        val current = _messages.value + userMsg
        _messages.value = current
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                if (prefs.stream) {
                    _messages.value = _messages.value + ChatMessage.text("assistant", "")
                    repo.sendStreaming(current, prefs.model).collect { chunk ->
                        val list = _messages.value.toMutableList()
                        val last = list.last()
                        val newText = last.displayText() + chunk
                        list[list.lastIndex] = ChatMessage.text("assistant", newText)
                        _messages.value = list
                    }
                    // if assistant msg ended empty, keep
                } else {
                    val reply = repo.send(current, prefs.model)
                    _messages.value = _messages.value + ChatMessage.text("assistant", reply)
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _loading.value = false
                saveHistory()
            }
        }
    }

    fun clear() {
        _messages.value = emptyList()
        saveHistory()
    }

    fun setModel(id: String) { prefs.model = id }

    fun refreshModels() {
        viewModelScope.launch {
            runCatching { repo.fetchModels() }
                .onSuccess { _models.value = it }
                .onFailure { _error.value = "Gagal load model: ${it.message}" }
        }
    }

    private fun loadHistory() {
        val json = prefs.history
        if (json.isBlank()) return
        runCatching {
            val type = object : TypeToken<List<ChatMessage>>() {}.type
            val list: List<ChatMessage> = gson.fromJson(json, type)
            _messages.value = list
        }
    }

    private fun saveHistory() {
        prefs.history = gson.toJson(_messages.value)
    }

    private fun encodeImage(resolver: android.content.ContentResolver, uri: Uri): String? {
        return try {
            val input = resolver.openInputStream(uri) ?: return null
            val bmp = android.graphics.BitmapFactory.decodeStream(input)
            input.close()
            if (bmp == null) return null
            val max = 1024
            val scaled = if (bmp.width > max || bmp.height > max) {
                val ratio = max.toFloat() / maxOf(bmp.width, bmp.height)
                android.graphics.Bitmap.createScaledBitmap(bmp, (bmp.width * ratio).toInt(), (bmp.height * ratio).toInt(), true)
            } else bmp
            val out = java.io.ByteArrayOutputStream()
            scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
            val b64 = android.util.Base64.encodeToString(out.toByteArray(), android.util.Base64.NO_WRAP)
            "data:image/jpeg;base64,$b64"
        } catch (e: Exception) {
            null
        }
    }
}
