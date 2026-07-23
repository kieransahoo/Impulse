package com.impulse.backend.collection

import com.impulse.backend.memory.MemorySourceParser
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LinkCollectionServiceTests {
    private val repository = mock(LinkCollectionRepository::class.java)
    private val collector = mock(YouTubePlaylistCollector::class.java)
    private val service = LinkCollectionService(repository, MemorySourceParser(), collector)

    @Test
    fun `collects and stores normalized YouTube playlist items`() {
        val userId = UUID.randomUUID()
        val playlistUrl = "https://www.youtube.com/playlist?list=PL123"
        `when`(repository.existsByUserIdAndSourceUrl(userId, playlistUrl)).thenReturn(false)
        `when`(collector.collect("PL123")).thenReturn(
            CollectedPlaylist(
                title = "Kotlin Shorts",
                items = listOf(
                    CollectedPlaylistItem(
                        videoId = "video-1",
                        title = "Kotlin in one minute",
                        description = "A short Kotlin tip",
                        thumbnailUrl = "https://img.youtube.com/video-1.jpg",
                        position = 0,
                    ),
                ),
            ),
        )
        `when`(repository.save(org.mockito.ArgumentMatchers.any(LinkCollection::class.java)))
            .thenAnswer { it.arguments.first() }

        val result = service.collect(CollectLinksRequest(userId, playlistUrl))

        assertEquals("Kotlin Shorts", result.title)
        assertEquals(1, result.itemCount)
        assertEquals("https://www.youtube.com/watch?v=video-1", result.items.single().url)
        val captor = ArgumentCaptor.forClass(LinkCollection::class.java)
        verify(repository).save(captor.capture())
        assertEquals(userId, captor.value.userId)
    }

    @Test
    fun `rejects Instagram for automatic collection`() {
        assertFailsWith<UnsupportedCollectionSourceException> {
            service.collect(
                CollectLinksRequest(
                    UUID.randomUUID(),
                    "https://instagram.com/reel/ABC123/",
                ),
            )
        }
    }

    @Test
    fun `imports playlist items collected by the OAuth extension without YouTube API access`() {
        val userId = UUID.randomUUID()
        val playlistUrl = "https://www.youtube.com/playlist?list=PL-OAUTH"
        `when`(repository.existsByUserIdAndSourceUrl(userId, playlistUrl)).thenReturn(false)
        `when`(repository.save(org.mockito.ArgumentMatchers.any(LinkCollection::class.java)))
            .thenAnswer { it.arguments.first() }

        val result = service.importCollected(
            ImportCollectedPlaylistRequest(
                userId = userId,
                url = playlistUrl,
                title = "Saved ideas",
                items = listOf(
                    ImportCollectedLinkRequest(
                        url = "https://www.youtube.com/watch?v=abc",
                        title = "Idea one",
                        description = "Useful idea",
                        thumbnailUrl = null,
                        position = 0,
                    ),
                ),
            ),
        )

        assertEquals("Saved ideas", result.title)
        assertEquals(1, result.itemCount)
        verify(repository).save(org.mockito.ArgumentMatchers.any(LinkCollection::class.java))
        verify(collector, org.mockito.Mockito.never()).collect(org.mockito.ArgumentMatchers.anyString())
    }
}
