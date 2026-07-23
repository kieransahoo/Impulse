package com.impulse.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.impulse.data.model.GroundingMemoryResponse
import com.impulse.data.repository.ImpulseRepository
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isUser: Boolean,
    val sources: List<GroundingMemoryResponse> = emptyList()
)

class ChatViewModel : ViewModel() {
    private val repository = ImpulseRepository()
    private val _messages = MutableStateFlow<List<UiMessage>>(emptyList())
    val messages: StateFlow<List<UiMessage>> = _messages.asStateFlow()
    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()
    private var userId: String? = null

    fun setUserId(value: String) {
        userId = value
    }

    fun sendMessage(text: String) {
        val currentUserId = userId
        if (text.isBlank() || currentUserId == null || _isTyping.value) return
        _messages.value += UiMessage(content = text, isUser = true)

        viewModelScope.launch {
            _isTyping.value = true
            repository.plan(currentUserId, text).fold(
                onSuccess = { result ->
                    val steps = result.plan.mapIndexed { index, step ->
                        buildString {
                            append("${index + 1}. ${step.step}")
                            step.durationMinutes?.let { append(" · $it min") }
                            step.reason?.takeIf(String::isNotBlank)?.let { append("\n$it") }
                        }
                    }.joinToString("\n\n")
                    val grounding = when {
                        result.groundingMemories.isEmpty() -> "General suggestion — no relevant saved memory was found."
                        else -> "Based on ${result.groundingMemories.size} saved ${if (result.groundingMemories.size == 1) "memory" else "memories"}."
                    }
                    _messages.value += UiMessage(
                        content = "${result.goal}\n\n${result.explanation}\n\n$steps\n\n$grounding",
                        isUser = false,
                        sources = result.groundingMemories
                    )
                },
                onFailure = {
                    _messages.value += UiMessage(
                        content = it.localizedMessage ?: "Could not create your plan. Please try again.",
                        isUser = false
                    )
                }
            )
            _isTyping.value = false
        }
    }

    fun clearConversation() {
        _messages.value = emptyList()
    }
}
