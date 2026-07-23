package com.impulse.backend.collection

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CollectLinksRequest(
    @field:NotNull
    val userId: UUID,

    @field:NotBlank
    @field:Size(max = 2_000)
    val url: String,
)

data class ProcessLinkCollectionRequest(
    @field:NotNull
    val userId: UUID,

    @field:Size(max = 2_000)
    val userNote: String? = null,
)

data class ImportCollectedPlaylistRequest(
    @field:NotNull
    val userId: UUID,

    @field:NotBlank
    @field:Size(max = 2_000)
    val url: String,

    @field:NotBlank
    @field:Size(max = 500)
    val title: String,

    @field:Size(min = 1, max = 500)
    val items: List<ImportCollectedLinkRequest>,
)

data class ImportCollectedLinkRequest(
    @field:NotBlank
    @field:Size(max = 2_000)
    val url: String,

    @field:NotBlank
    @field:Size(max = 500)
    val title: String,

    @field:Size(max = 10_000)
    val description: String? = null,

    @field:Size(max = 2_000)
    val thumbnailUrl: String? = null,

    val position: Int,
)

data class CollectedLinkResponse(
    val url: String,
    val title: String,
    val description: String?,
    val thumbnailUrl: String?,
    val position: Int,
)

data class LinkCollectionResponse(
    val id: UUID,
    val userId: UUID,
    val sourceUrl: String,
    val title: String,
    val status: LinkCollectionStatus,
    val itemCount: Int,
    val items: List<CollectedLinkResponse>,
    val createdAt: Instant,
)

fun LinkCollection.toResponse() = LinkCollectionResponse(
    id = id,
    userId = userId,
    sourceUrl = sourceUrl,
    title = title,
    status = status,
    itemCount = items.size,
    items = items.map {
        CollectedLinkResponse(
            url = it.url,
            title = it.title,
            description = it.description,
            thumbnailUrl = it.thumbnailUrl,
            position = it.position,
        )
    },
    createdAt = createdAt,
)
