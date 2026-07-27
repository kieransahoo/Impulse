package com.impulse.backend.savedplan

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class RegeneratePlanRequest(
    @field:NotNull
    val userId: UUID,

    val constraints: Map<String, Any?> = emptyMap(),

    val allowGeneralKnowledge: Boolean = false,

    @field:Size(max = 2_000)
    val query: String? = null,
)

data class SavePlanRequest(
    @field:NotNull
    val userId: UUID,

    @field:NotBlank
    @field:Size(max = 500)
    val goal: String,

    @field:NotBlank
    @field:Size(max = 10_000)
    val explanation: String,

    @field:NotEmpty
    @field:Size(max = 100)
    val plan: List<@Valid SavePlanStepRequest>,

    @field:Size(max = 100)
    val retrievedMemoryIds: List<UUID> = emptyList(),
)

data class SavePlanStepRequest(
    @field:NotBlank
    @field:Size(max = 2_000)
    val step: String,

    @field:Min(1)
    @field:Max(100_000)
    val durationMinutes: Int? = null,

    @field:Size(max = 5_000)
    val reason: String? = null,

    @field:Size(max = 20)
    val memoryIds: List<UUID> = emptyList(),
)

data class SavedPlanResponse(
    val id: UUID,
    val userId: UUID,
    val goal: String,
    val explanation: String,
    val plan: List<SavedPlanStepResponse>,
    val retrievedMemoryIds: List<UUID>,
    val createdAt: Instant,
    val status: SavedPlanStatus = SavedPlanStatus.SAVED,
    val activatedAt: Instant? = null,
    val completedAt: Instant? = null,
)

data class SavedPlanStepResponse(
    val id: UUID,
    val order: Int,
    val step: String,
    val durationMinutes: Int?,
    val reason: String?,
    val memoryIds: List<UUID>,
    val completed: Boolean = false,
    val completedAt: Instant? = null,
)

data class UpdateStepCompletionRequest(
    val completed: Boolean,
)
