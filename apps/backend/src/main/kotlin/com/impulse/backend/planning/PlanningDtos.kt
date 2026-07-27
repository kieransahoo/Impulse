package com.impulse.backend.planning

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

data class CreatePlanRequest(
    @field:NotNull
    val userId: UUID,

    @field:NotBlank
    @field:Size(max = 2_000)
    val query: String,

    val constraints: Map<String, Any?> = emptyMap(),

    val allowGeneralKnowledge: Boolean = false,
)

enum class PlanIntent {
    STUDY,
    WORKOUT,
    MEAL,
    ROOM,
    PRODUCT,
    OUTING,
    LEARNING,
    PROJECT,
    ROUTINE,
    GENERAL,
}

enum class GroundingStatus {
    STRONG_GROUNDING,
    PARTIAL_GROUNDING,
    NO_GROUNDING,
}

enum class PlanSourceType {
    MEMORY,
    GENERAL,
}

data class PlanStepResponse(
    val step: String,
    val durationMinutes: Int?,
    val reason: String?,
    val memoryIds: List<UUID>,
    val sourceType: PlanSourceType,
)

data class CreatePlanResponse(
    val intent: PlanIntent,
    val groundingStatus: GroundingStatus,
    val goal: String,
    val explanation: String,
    val plan: List<PlanStepResponse>,
    val retrievedMemoryIds: List<UUID>,
    val groundingMemories: List<GroundingMemoryResponse>,
    val missingContext: List<String> = emptyList(),
    val suggestedSources: List<String> = emptyList(),
)

data class GroundingMemoryResponse(
    val id: UUID,
    val title: String,
    val summary: String,
    val sourceUrl: String,
    val thumbnailUrl: String?,
    val platform: String,
)

data class AiQueryEmbeddingRequest(val query: String)
data class AiQueryEmbeddingResponse(val embedding: List<Float>)

data class AiPlanRequest(
    val query: String,
    val memories: List<AiPlanningMemory>,
    val constraints: Map<String, Any?>,
    val intent: PlanIntent,
    val groundingStatus: GroundingStatus,
    val allowGeneralKnowledge: Boolean,
    val missingContext: List<String>,
)

data class AiPlanningMemory(
    val id: String,
    val title: String,
    val summary: String,
    val category: String,
    val topics: Set<String>,
    val actions: List<AiPlanningAction>,
    val sourceUrl: String,
)

data class AiPlanningAction(
    val action: String,
    val useWhen: List<String>,
    val durationMinutes: Int?,
    val category: String?,
)

data class AiPlanResponse(
    val goal: String,
    val explanation: String,
    val plan: List<AiPlanStep>,
)

data class AiPlanStep(
    val step: String,
    val durationMinutes: Int?,
    val reason: String?,
    val memoryIds: List<String> = emptyList(),
    val sourceType: PlanSourceType = PlanSourceType.MEMORY,
)
