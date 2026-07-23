package com.impulse.backend.usercollection

import com.impulse.backend.memory.MemorySourceParser
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class UserCollectionServiceTests {
    private val collections = mock(UserCollectionRepository::class.java)
    private val sources = mock(UserCollectionSourceRepository::class.java)
    private val processor = mock(UserCollectionSourceProcessor::class.java)
    private val service = UserCollectionService(
        collections,
        sources,
        MemorySourceParser(),
        processor,
    )

    @Test
    fun `reuses an existing collection with the same name`() {
        val userId = UUID.randomUUID()
        val existing = UserCollection(userId = userId, name = "Travel", description = null)
        `when`(collections.findByUserIdAndNameIgnoreCase(userId, "travel")).thenReturn(existing)
        `when`(sources.existsByCollectionIdAndUserIdAndUrl(existing.id, userId, URL)).thenReturn(true)
        `when`(sources.findAllByCollectionIdAndUserIdOrderByCreatedAtAsc(existing.id, userId))
            .thenReturn(emptyList())

        val result = service.create(
            CreateUserCollectionRequest(
                userId = userId,
                name = "travel",
                sources = listOf(SharedSourceRequest(URL)),
            ),
        )

        assertEquals(existing.id, result.id)
        assertEquals(0, result.totalSources)
    }

    @Test
    fun `defaults a single shared link to ALL without duplicating its source`() {
        val userId = UUID.randomUUID()
        val all = UserCollection(userId = userId, name = "ALL", description = null)
        `when`(collections.findByUserIdAndNameIgnoreCase(userId, "ALL")).thenReturn(all)
        `when`(sources.existsByCollectionIdAndUserIdAndUrl(all.id, userId, URL)).thenReturn(true)
        `when`(sources.findAllByCollectionIdAndUserIdOrderByCreatedAtAsc(all.id, userId))
            .thenReturn(emptyList())

        val result = service.addSource(AddSourceToCollectionRequest(userId = userId, url = URL))

        assertEquals("ALL", result.name)
        assertEquals(0, result.totalSources)
    }

    private companion object {
        const val URL = "https://example.com/article"
    }
}
