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
        val normalizedName = request.name.trim()
        val collection = collectionRepository.findByUserIdAndNameIgnoreCase(
            request.userId,
            normalizedName,
        ) ?: collectionRepository.save(
            UserCollection(
                userId = request.userId,
                name = normalizedName,
                description = request.description?.trim()?.takeIf(String::isNotBlank),
            ),
        )
        addSources(collection, request.sources)
        return response(collection)
    }

    fun addSource(request: AddSourceToCollectionRequest): UserCollectionResponse {
        val collection = request.collectionId?.let {
            collectionRepository.findByIdAndUserId(it, request.userId)
                ?: throw UserCollectionNotFoundException(it)
        } ?: getOrCreateAll(request.userId)
        addSources(
            collection,
            listOf(SharedSourceRequest(request.url, request.userNote, request.content)),
        )
        return response(collection)
    }

    private fun getOrCreateAll(userId: UUID): UserCollection =
        collectionRepository.findByUserIdAndNameIgnoreCase(userId, DEFAULT_COLLECTION)
            ?: collectionRepository.save(
                UserCollection(
                    userId = userId,
                    name = DEFAULT_COLLECTION,
                    description = "Everything you save, ready to retrieve.",
                ),
            )

    private fun addSources(collection: UserCollection, inputs: List<SharedSourceRequest>) {
        val distinctSources = inputs
            .map { it to sourceParser.parse(it.url).normalizedUrl }
            .distinctBy(Pair<SharedSourceRequest, String>::second)
            .filterNot { (_, normalizedUrl) ->
                sourceRepository.existsByCollectionIdAndUserIdAndUrl(
                    collection.id,
                    collection.userId,
                    normalizedUrl,
                )
            }
        val saved = distinctSources.map { (input, normalizedUrl) ->
            sourceRepository.save(
                UserCollectionSource(
                    collectionId = collection.id,
                    userId = collection.userId,
                    url = normalizedUrl,
                    userNote = input.userNote?.trim()?.takeIf(String::isNotBlank),
                ),
            ) to input.content
        }
        saved.forEach { (source, content) -> processor.process(source.id, content) }
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

    private companion object {
        const val DEFAULT_COLLECTION = "ALL"
    }
}

class UserCollectionNotFoundException(id: UUID) :
    NoSuchElementException("Collection $id was not found")
