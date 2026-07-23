package com.impulse.backend.usercollection

import com.impulse.backend.memory.ImportMemoryRequest
import com.impulse.backend.memory.MemoryService
import com.impulse.backend.memory.MemorySourceParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class UserCollectionSourceProcessor(
    private val sourceRepository: UserCollectionSourceRepository,
    private val memoryService: MemoryService,
) {
    fun process(sourceId: UUID, content: String?) {
        val source = sourceRepository.findById(sourceId).orElseThrow()
        try {
            val memory = memoryService.importShared(
                ImportMemoryRequest(
                    userId = source.userId,
                    url = source.url,
                    userNote = source.userNote,
                    content = content,
                ),
            )
            source.status = CollectionSourceStatus.PROCESSED
            source.memoryId = memory.id
            source.errorMessage = null
        } catch (exception: Exception) {
            source.status = CollectionSourceStatus.FAILED
            source.errorMessage = userSafeMessage(exception)
        }
        source.processedAt = Instant.now()
        sourceRepository.save(source)
    }

    private fun userSafeMessage(exception: Exception): String =
        (exception.message ?: "Source processing failed").take(1_000)
}

@Service
class UserCollectionService(
    private val collectionRepository: UserCollectionRepository,
    private val sourceRepository: UserCollectionSourceRepository,
    private val sourceParser: MemorySourceParser,
    private val processor: UserCollectionSourceProcessor,
) {
    fun create(request: CreateUserCollectionRequest): UserCollectionResponse {
        val distinctSources = request.sources
            .map { it to sourceParser.parse(it.url).normalizedUrl }
            .distinctBy(Pair<SharedSourceRequest, String>::second)
        val collection = collectionRepository.save(
            UserCollection(
                userId = request.userId,
                name = request.name.trim(),
                description = request.description?.trim()?.takeIf(String::isNotBlank),
            ),
        )
        val saved = distinctSources.map { (input, normalizedUrl) ->
            sourceRepository.save(
                UserCollectionSource(
                    collectionId = collection.id,
                    userId = request.userId,
                    url = normalizedUrl,
                    userNote = input.userNote?.trim()?.takeIf(String::isNotBlank),
                ),
            ) to input.content
        }
        saved.forEach { (source, content) -> processor.process(source.id, content) }
        return response(collection)
    }

    @Transactional(readOnly = true)
    fun findAll(userId: UUID): List<UserCollectionResponse> =
        collectionRepository.findAllByUserIdOrderByCreatedAtDesc(userId).map(::response)

    @Transactional(readOnly = true)
    fun findOne(id: UUID, userId: UUID): UserCollectionResponse =
        collectionRepository.findByIdAndUserId(id, userId)
            ?.let(::response)
            ?: throw UserCollectionNotFoundException(id)

    private fun response(collection: UserCollection): UserCollectionResponse {
        val sources = sourceRepository.findAllByCollectionIdAndUserIdOrderByCreatedAtAsc(
            collection.id,
            collection.userId,
        )
        return UserCollectionResponse(
            id = collection.id,
            userId = collection.userId,
            name = collection.name,
            description = collection.description,
            totalSources = sources.size,
            processedSources = sources.count { it.status == CollectionSourceStatus.PROCESSED },
            failedSources = sources.count { it.status == CollectionSourceStatus.FAILED },
            sources = sources.map {
                CollectionSourceResponse(
                    id = it.id,
                    url = it.url,
                    userNote = it.userNote,
                    status = it.status,
                    memoryId = it.memoryId,
                    errorMessage = it.errorMessage,
                    processedAt = it.processedAt,
                )
            },
            createdAt = collection.createdAt,
        )
    }
}

class UserCollectionNotFoundException(id: UUID) :
    NoSuchElementException("Collection $id was not found")
