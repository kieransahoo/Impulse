package com.impulse.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.impulse.data.model.ChatMessage
import com.impulse.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

// ─── UI Message model ─────────────────────────────────────────────────────────

data class UiMessage(
    val id      : String  = UUID.randomUUID().toString(),
    val content : String,
    val isUser  : Boolean
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

class ChatViewModel : ViewModel() {

    private val repository = ChatRepository()

    /** Messages displayed in the RecyclerView */
    private val _messages = MutableStateFlow<List<UiMessage>>(emptyList())
    val messages: StateFlow<List<UiMessage>> = _messages.asStateFlow()

    /** Whether the AI is currently generating a response */
    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    /** Full conversation history sent to OpenAI on each turn */
    private val history = mutableListOf<ChatMessage>()

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        // 1. Add user bubble immediately
        _messages.value += UiMessage(content = text, isUser = true)
        history.add(ChatMessage(role = "user", content = text))

        // 2. Call OpenAI in background
        viewModelScope.launch {
            _isTyping.value = true
            val result = repository.sendMessage(history.toList())
            _isTyping.value = false

            result.fold(
                onSuccess = { reply ->
                    _messages.value += UiMessage(content = reply, isUser = false)
                    history.add(ChatMessage(role = "assistant", content = reply))
                },
                onFailure = { error ->
                    _messages.value += UiMessage(
                        content = "⚠️ ${error.message ?: "Something went wrong. Please try again."}",
                        isUser  = false
                    )
                }
            )
        }
    }

    fun clearConversation() {
        _messages.value = emptyList()
        history.clear()
    }
}
