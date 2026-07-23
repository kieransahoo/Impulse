package com.impulse.backend.usercollection

import com.impulse.backend.memory.ImportMemoryRequest
import com.impulse.backend.memory.MemoryActionResponse
import com.impulse.backend.memory.MemoryPlatform
import com.impulse.backend.memory.MemoryResponse
import com.impulse.backend.memory.MemoryService
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Instant
import java.util.Optional
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UserCollectionSourceProcessorTests {
    @Test
    fun `links a processed collection source to its memory`() {
        val repository = mock(UserCollectionSourceRepository::class.java)
        val memoryService = mock(MemoryService::class.java)
        val source = source()
        val memoryId = UUID.randomUUID()
        `when`(repository.findById(source.id)).thenReturn(Optional.of(source))
        `when`(memoryService.importShared(requestFor(source))).thenReturn(
            memory(memoryId, source.userId, source.url),
        )
        `when`(repository.save(any(UserCollectionSource::class.java))).thenAnswer { it.arguments[0] }

        UserCollectionSourceProcessor(repository, memoryService).process(source.id, null)

        assertEquals(CollectionSourceStatus.PROCESSED, source.status)
        assertEquals(memoryId, source.memoryId)
        assertNotNull(source.processedAt)
        verify(repository).save(source)
    }

    @Test
    fun `records one failed source without throwing away the collection`() {
        val repository = mock(UserCollectionSourceRepository::class.java)
        val memoryService = mock(MemoryService::class.java)
        val source = source()
        `when`(repository.findById(source.id)).thenReturn(Optional.of(source))
        `when`(memoryService.importShared(requestFor(source)))
            .thenThrow(IllegalStateException("AI source could not be processed"))
        `when`(repository.save(any(UserCollectionSource::class.java))).thenAnswer { it.arguments[0] }

        UserCollectionSourceProcessor(repository, memoryService).process(source.id, null)

        assertEquals(CollectionSourceStatus.FAILED, source.status)
        assertEquals("AI source could not be processed", source.errorMessage)
        assertNotNull(source.processedAt)
    }

    private fun source() = UserCollectionSource(
        collectionId = UUID.randomUUID(),
        userId = UUID.randomUUID(),
        url = "https://example.com/article",
        userNote = null,
    )

    private fun requestFor(source: UserCollectionSource) = ImportMemoryRequest(
        userId = source.userId,
        url = source.url,
        userNote = null,
        content = null,
    )

    private fun memory(id: UUID, userId: UUID, url: String) = MemoryResponse(
        id = id,
        userId = userId,
        sourceUrl = url,
        platform = MemoryPlatform.WEB,
        title = "Example",
        description = null,
        summary = "Example memory",
        category = "general",
        tags = emptySet(),
        topics = emptySet(),
        actions = emptyList<MemoryActionResponse>(),
        userNote = null,
        createdAt = Instant.now(),
    )
}
