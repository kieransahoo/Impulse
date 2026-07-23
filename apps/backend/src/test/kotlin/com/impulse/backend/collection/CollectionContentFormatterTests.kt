package com.impulse.backend.collection

import com.impulse.backend.memory.MemoryPlatform
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class CollectionContentFormatterTests {
    private val formatter = CollectionContentFormatter()

    @Test
    fun `formats collected items as bounded plain text for the AI service`() {
        val collection = LinkCollection(
            userId = UUID.randomUUID(),
            sourceUrl = "https://youtube.com/playlist?list=PL123",
            platform = MemoryPlatform.YOUTUBE_PLAYLIST,
            title = "Kotlin Shorts",
            items = mutableListOf(
                CollectedLink(
                    url = "https://youtube.com/watch?v=one",
                    title = "Kotlin tip",
                    description = "Use data classes for value objects.",
                    thumbnailUrl = null,
                    position = 0,
                ),
            ),
        )

        val content = formatter.format(collection)

        assertContains(content, "Collection: Kotlin Shorts")
        assertContains(content, "1. Kotlin tip")
        assertContains(content, "https://youtube.com/watch?v=one")
        assertTrue(content.length <= 50_000)
    }

    @Test
    fun `truncates oversized third party content`() {
        val collection = LinkCollection(
            userId = UUID.randomUUID(),
            sourceUrl = "https://youtube.com/playlist?list=PL123",
            platform = MemoryPlatform.YOUTUBE_PLAYLIST,
            title = "Large playlist",
            items = mutableListOf(
                CollectedLink(
                    url = "https://youtube.com/watch?v=one",
                    title = "Large description",
                    description = "x".repeat(60_000),
                    thumbnailUrl = null,
                    position = 0,
                ),
            ),
        )

        assertTrue(formatter.format(collection).length <= 50_000)
    }
}
