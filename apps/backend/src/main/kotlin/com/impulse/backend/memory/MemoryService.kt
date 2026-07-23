package com.impulse.backend.memory

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class MemoryService(
    private val repository: MemoryRepository,
    private val sourceParser: MemorySourceParser,
    private val aiProcessor: AiMemoryProcessor,
) {
    @Transactional
    fun import(request: ImportMemoryRequest): MemoryResponse {
        val source = sourceParser.parse(request.url)
        if (repository.existsByUserIdAndSourceUrl(request.userId, source.normalizedUrl)) {
            throw DuplicateMemoryException()
        }

        val result = aiProcessor.process(
            AiMemoryRequest(
                sourceUrl = source.normalizedUrl,
                platform = source.platform,
                userNote = request.userNote,
                content = request.content,
            ),
        )

        val memory = Memory(
            userId = request.userId,
            sourceUrl = source.normalizedUrl,
            platform = source.platform,
            title = result.title.trim(),
            description = result.description?.trim(),
            thumbnailUrl = result.thumbnailUrl?.trim()?.takeIf(String::isNotBlank),
            summary = result.summary.trim(),
            category = result.category.trim(),
            tags = result.tags.map(String::trim).filter(String::isNotBlank).toMutableSet(),
            topics = result.topics.map(String::trim).filter(String::isNotBlank).toMutableSet(),
            actions = result.actions
                .filter { it.action.isNotBlank() }
                .map {
                    MemoryAction(
                        action = it.action.trim(),
                        useWhen = it.useWhen.joinToString("|").ifBlank { null },
                        durationMinutes = it.durationMinutes,
                        category = it.category?.trim(),
                    )
                }
                .toMutableList(),
            userNote = request.userNote?.trim(),
            embedding = result.embedding.toFloatArray(),
        )
        return repository.save(memory).toResponse()
    }

    @Transactional
    fun importShared(request: ImportMemoryRequest): MemoryResponse {
        val source = sourceParser.parse(request.url)
        return repository.findByUserIdAndSourceUrl(request.userId, source.normalizedUrl)
            ?.toResponse()
            ?: import(request)
    }

    @Transactional(readOnly = true)
    fun findAll(userId: UUID): List<MemoryResponse> =
        repository.findAllByUserIdOrderByCreatedAtDesc(userId).map(Memory::toResponse)
}

class DuplicateMemoryException :
    IllegalStateException("This link is already stored for the user")
