package com.impulse.backend.usercollection

import com.impulse.backend.memory.ImportMemoryRequest
import com.impulse.backend.memory.MemoryService
import com.impulse.backend.memory.MemorySourceParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Executor

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

    fun markFailed(sourceId: UUID, message: String) {
        val source = sourceRepository.findById(sourceId).orElseThrow()
        source.status = CollectionSourceStatus.FAILED
        source.errorMessage = message.take(1_000)
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
    private val collectionSourceExecutor: Executor,
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

    @Transactional
    fun update(id: UUID, request: UpdateUserCollectionRequest): UserCollectionResponse {
        val collection = collectionRepository.findByIdAndUserId(id, request.userId)
            ?: throw UserCollectionNotFoundException(id)
        if (collection.name.equals(DEFAULT_COLLECTION, ignoreCase = true)) {
            throw DefaultCollectionMutationException()
        }
        val requestedName = request.name.trim()
        val conflict = collectionRepository.findByUserIdAndNameIgnoreCase(request.userId, requestedName)
        if (conflict != null && conflict.id != collection.id) {
            throw CollectionNameConflictException()
        }
        collection.name = requestedName
        collection.description = request.description?.trim()?.takeIf(String::isNotBlank)
        return response(collectionRepository.save(collection))
    }

    @Transactional
    fun delete(id: UUID, userId: UUID) {
        val collection = collectionRepository.findByIdAndUserId(id, userId)
            ?: throw UserCollectionNotFoundException(id)
        if (collection.name.equals(DEFAULT_COLLECTION, ignoreCase = true)) {
            throw DefaultCollectionMutationException()
        }
        sourceRepository.deleteAllByCollectionIdAndUserId(id, userId)
        collectionRepository.delete(collection)
    }

    @Transactional
    fun removeSource(collectionId: UUID, sourceId: UUID, userId: UUID) {
        val collection = collectionRepository.findByIdAndUserId(collectionId, userId)
            ?: throw UserCollectionNotFoundException(collectionId)
        val source = sourceRepository.findByIdAndCollectionIdAndUserId(sourceId, collectionId, userId)
            ?: throw CollectionSourceNotFoundException(sourceId)
        sourceRepository.delete(source)
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
        saved.forEach { (source, content) ->
            runCatching {
                collectionSourceExecutor.execute {
                    processor.process(source.id, content)
                }
            }.onFailure {
                processor.markFailed(
                    source.id,
                    "Memory processing is busy. Please retry this source.",
                )
            }
        }
    }

    @Transactional
    fun findAll(userId: UUID): List<UserCollectionResponse> {
        getOrCreateAll(userId)
        return collectionRepository.findAllByUserIdOrderByCreatedAtDesc(userId).map(::response)
    }

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

class CollectionSourceNotFoundException(id: UUID) :
    NoSuchElementException("Collection source $id was not found")

class CollectionNameConflictException :
    IllegalStateException("A collection with this name already exists")

class DefaultCollectionMutationException :
    IllegalStateException("The ALL collection cannot be renamed or deleted")
