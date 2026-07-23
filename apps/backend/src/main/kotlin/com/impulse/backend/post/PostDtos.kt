package com.impulse.backend.post

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreatePostRequest(
    @field:NotBlank
    @field:Size(max = 2_000)
    val caption: String,

    @field:NotBlank
    @field:Size(max = 2_000)
    val image: String,

    @field:NotNull
    val category: PostCategory,

    @field:Size(max = 30)
    val tags: Set<String> = emptySet(),
)

data class UpdatePostRequest(
    @field:NotBlank
    @field:Size(max = 2_000)
    val caption: String,

    @field:NotBlank
    @field:Size(max = 2_000)
    val image: String,

    @field:NotNull
    val category: PostCategory,

    @field:Size(max = 30)
    val tags: Set<String> = emptySet(),
)

data class PostResponse(
    val id: UUID,
    val caption: String,
    val image: String,
    val category: PostCategory,
    val tags: Set<String>,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun Post.toResponse() = PostResponse(
    id = id,
    caption = caption,
    image = imageUrl,
    category = category,
    tags = tags,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
