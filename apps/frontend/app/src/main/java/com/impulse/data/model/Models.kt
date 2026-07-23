package com.impulse.data.model

// ─── Share ────────────────────────────────────────────────────────────────────

data class ShareRequest(
    val url: String,
    val source: String = "android_share",   // always "android_share" for this client
    val userId: String? = null
)

data class ShareResponse(
    val success: Boolean,
    val message: String
)

// ─── OpenAI Chat Completions ──────────────────────────────────────────────────

data class ChatMessage(
    val role: String,       // "system" | "user" | "assistant"
    val content: String
)

data class ChatRequest(
    val model: String = "gpt-3.5-turbo",
    val messages: List<ChatMessage>,
    val max_tokens: Int = 1024,
    val temperature: Double = 0.7
)

data class ChatChoice(
    val index: Int,
    val message: ChatMessage,
    val finish_reason: String?
)

data class ChatUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

data class ChatResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<ChatChoice>,
    val usage: ChatUsage
)

// ─── User Session ─────────────────────────────────────────────────────────────

data class UserSession(
    val userId: String,
    val email: String,
    val displayName: String,
    val photoUrl: String?,
    val idToken: String
)
