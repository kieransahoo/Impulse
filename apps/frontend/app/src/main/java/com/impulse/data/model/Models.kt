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

data class RegisterRequest(
    val email: String,
    val displayName: String,
    val password: String
)

data class LoginRequest(val email: String, val password: String)

data class AuthResponse(
    val userId: String,
    val email: String,
    val displayName: String,
    val token: String,
    val expiresAt: String
)

// ─── Impulse backend MVP ─────────────────────────────────────────────────────

data class SharedSourceRequest(val url: String, val userNote: String? = null)

data class CreateCollectionRequest(
    val userId: String,
    val name: String,
    val description: String? = null,
    val sources: List<SharedSourceRequest>
)

data class AddSourceToCollectionRequest(
    val userId: String,
    val collectionId: String? = null,
    val url: String,
    val userNote: String? = null
)

data class CollectionSourceResponse(
    val id: String,
    val url: String,
    val status: String,
    val memoryId: String?,
    val errorMessage: String?
)

data class UserCollectionResponse(
    val id: String,
    val userId: String,
    val name: String,
    val description: String?,
    val totalSources: Int,
    val processedSources: Int,
    val failedSources: Int,
    val sources: List<CollectionSourceResponse>,
    val createdAt: String
)

data class MemoryResponse(
    val id: String,
    val sourceUrl: String,
    val platform: String? = null,
    val title: String,
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val summary: String,
    val category: String,
    val score: Double? = null
)

data class SearchMemoriesRequest(
    val userId: String,
    val query: String,
    val limit: Int = 8
)

data class CreatePlanRequest(
    val userId: String,
    val query: String,
    val constraints: Map<String, Any?> = emptyMap()
)

data class PlanStepResponse(
    val step: String,
    val durationMinutes: Int?,
    val reason: String?,
    val memoryIds: List<String>
)

data class GroundingMemoryResponse(
    val id: String,
    val title: String,
    val summary: String,
    val sourceUrl: String,
    val thumbnailUrl: String?,
    val platform: String
)

data class PlanResponse(
    val goal: String,
    val explanation: String,
    val plan: List<PlanStepResponse>,
    val retrievedMemoryIds: List<String>,
    val groundingMemories: List<GroundingMemoryResponse>
)
