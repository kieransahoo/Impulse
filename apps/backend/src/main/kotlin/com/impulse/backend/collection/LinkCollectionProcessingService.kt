package com.impulse.backend.collection

import com.impulse.backend.memory.ImportMemoryRequest
import com.impulse.backend.memory.MemoryResponse
import com.impulse.backend.memory.MemoryService
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class CollectionContentFormatter {
    fun format(collection: LinkCollection): String {
        val content = buildString {
            appendLine("Collection: ${collection.title}")
            appendLine("Source: ${collection.sourceUrl}")
            appendLine("Items: ${collection.items.size}")
            collection.items.forEach { item ->
                appendLine()
                appendLine("${item.position + 1}. ${item.title}")
                appendLine("URL: ${item.url}")
                item.description
                    ?.takeIf(String::isNotBlank)
                    ?.let { appendLine("Description: $it") }
            }
        }
        return content.take(MAX_CONTENT_LENGTH)
    }

    private companion object {
        const val MAX_CONTENT_LENGTH = 50_000
    }
}

@Service
class LinkCollectionProcessingService(
    private val repository: LinkCollectionRepository,
    private val memoryService: MemoryService,
    private val formatter: CollectionContentFormatter,
) {
    @Transactional
    fun process(collectionId: UUID, userId: UUID, userNote: String?): MemoryResponse {
        val collection = repository.findByIdAndUserId(collectionId, userId)
            ?: throw LinkCollectionNotFoundException(collectionId)

        val memory = memoryService.import(
            ImportMemoryRequest(
                userId = userId,
                url = collection.sourceUrl,
                userNote = userNote,
                content = formatter.format(collection),
            ),
        )
        collection.status = LinkCollectionStatus.PROCESSED
        return memory
    }
}

class LinkCollectionNotFoundException(id: UUID) :
    NoSuchElementException("Link collection $id was not found")
