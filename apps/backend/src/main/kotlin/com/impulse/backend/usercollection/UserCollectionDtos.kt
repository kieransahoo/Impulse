package com.impulse.backend.usercollection

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateUserCollectionRequest(
    @field:NotNull
    val userId: UUID,

    @field:NotBlank
    @field:Size(max = 200)
    val name: String,

    @field:Size(max = 2_000)
    val description: String? = null,

    @field:NotEmpty
    @field:Size(max = 20)
    val sources: List<@Valid SharedSourceRequest>,
)

data class SharedSourceRequest(
    @field:NotBlank
    @field:Size(max = 2_000)
    val url: String,

    @field:Size(max = 2_000)
    val userNote: String? = null,

    @field:Size(max = 50_000)
    val content: String? = null,
)

data class AddSourceToCollectionRequest(
    @field:NotNull
    val userId: UUID,

    val collectionId: UUID? = null,

    @field:NotBlank
    @field:Size(max = 2_000)
    val url: String,

    @field:Size(max = 2_000)
    val userNote: String? = null,

    @field:Size(max = 50_000)
    val content: String? = null,
)

data class UserCollectionResponse(
    val id: UUID,
    val userId: UUID,
    val name: String,
    val description: String?,
    val totalSources: Int,
    val processedSources: Int,
    val failedSources: Int,
    val sources: List<CollectionSourceResponse>,
    val createdAt: Instant,
)

data class CollectionSourceResponse(
    val id: UUID,
    val url: String,
    val userNote: String?,
    val status: CollectionSourceStatus,
    val memoryId: UUID?,
    val errorMessage: String?,
    val processedAt: Instant?,
)
