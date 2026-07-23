package com.impulse.backend.memory

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class ImportMemoryRequest(
    @field:NotNull
    val userId: UUID,

    @field:NotBlank
    @field:Size(max = 2_000)
    val url: String,

    @field:Size(max = 2_000)
    val userNote: String? = null,

    @field:Size(max = 50_000)
    val content: String? = null,
)

data class MemoryResponse(
    val id: UUID,
    val userId: UUID,
    val sourceUrl: String,
    val platform: MemoryPlatform,
    val title: String,
    val description: String?,
    val thumbnailUrl: String? = null,
    val summary: String,
    val category: String,
    val tags: Set<String>,
    val topics: Set<String>,
    val actions: List<MemoryActionResponse>,
    val userNote: String?,
    val createdAt: Instant,
)

data class MemoryActionResponse(
    val action: String,
    val useWhen: List<String>,
    val durationMinutes: Int?,
    val category: String?,
)

fun Memory.toResponse() = MemoryResponse(
    id = id,
    userId = userId,
    sourceUrl = sourceUrl,
    platform = platform,
    title = title,
    description = description,
    thumbnailUrl = thumbnailUrl,
    summary = summary,
    category = category,
    tags = tags,
    topics = topics,
    actions = actions.map {
        MemoryActionResponse(
            action = it.action,
            useWhen = it.useWhen?.split("|")?.filter(String::isNotBlank) ?: emptyList(),
            durationMinutes = it.durationMinutes,
            category = it.category,
        )
    },
    userNote = userNote,
    createdAt = createdAt,
)
