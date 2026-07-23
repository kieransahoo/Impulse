package com.impulse.backend.planning

import com.impulse.backend.memory.MemoryActionResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class SearchMemoriesRequest(
    @field:NotNull
    val userId: UUID,

    @field:NotBlank
    @field:Size(max = 2_000)
    val query: String,

    @field:Min(1)
    @field:Max(20)
    val limit: Int = 8,
)

data class RetrievedMemoryResponse(
    val id: UUID,
    val sourceUrl: String,
    val title: String,
    val summary: String,
    val thumbnailUrl: String?,
    val category: String,
    val tags: Set<String>,
    val topics: Set<String>,
    val actions: List<MemoryActionResponse>,
    val score: Double,
    val semanticScore: Double,
    val keywordScore: Double,
    val recencyScore: Double,
)

@RestController
@RequestMapping("/api/memories")
class MemoryRetrievalController(
    private val service: MemoryRetrievalService,
) {
    @PostMapping("/search")
    fun search(@Valid @RequestBody request: SearchMemoriesRequest): List<RetrievedMemoryResponse> =
        service.retrieve(request.userId, request.query, request.limit).map { result ->
            val memory = result.memory
            RetrievedMemoryResponse(
                id = memory.id,
                sourceUrl = memory.sourceUrl,
                title = memory.title,
                summary = memory.summary,
                thumbnailUrl = memory.thumbnailUrl,
                category = memory.category,
                tags = memory.tags,
                topics = memory.topics,
                actions = memory.actions.map {
                    MemoryActionResponse(
                        action = it.action,
                        useWhen = it.useWhen?.split("|")?.filter(String::isNotBlank) ?: emptyList(),
                        durationMinutes = it.durationMinutes,
                        category = it.category,
                    )
                },
                score = result.score,
                semanticScore = result.semanticScore,
                keywordScore = result.keywordScore,
                recencyScore = result.recencyScore,
            )
        }
}
