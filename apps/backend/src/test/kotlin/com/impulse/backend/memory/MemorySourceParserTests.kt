package com.impulse.backend.memory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MemorySourceParserTests {
    private val parser = MemorySourceParser()

    @Test
    fun `accepts a YouTube playlist URL`() {
        val result = parser.parse("https://www.youtube.com/playlist?list=PL123")

        assertEquals(MemoryPlatform.YOUTUBE_PLAYLIST, result.platform)
        assertEquals("PL123", parser.youtubePlaylistId(result.normalizedUrl))
    }

    @Test
    fun `accepts Instagram posts and reels`() {
        assertEquals(
            MemoryPlatform.INSTAGRAM,
            parser.parse("https://www.instagram.com/p/ABC123/").platform,
        )
        assertEquals(
            MemoryPlatform.INSTAGRAM,
            parser.parse("https://instagram.com/reel/XYZ789/").platform,
        )
    }

    @Test
    fun `accepts YouTube videos and Shorts`() {
        assertEquals(
            MemoryPlatform.YOUTUBE_VIDEO,
            parser.parse("https://youtube.com/watch?v=abc").platform,
        )
        assertEquals(
            MemoryPlatform.YOUTUBE_VIDEO,
            parser.parse("https://youtube.com/shorts/abc").platform,
        )
        assertEquals(
            MemoryPlatform.YOUTUBE_VIDEO,
            parser.parse("https://youtu.be/abc").platform,
        )
    }

    @Test
    fun `accepts a generic public web URL as WEB`() {
        val result = parser.parse("https://example.com/post#section")

        assertEquals(MemoryPlatform.WEB, result.platform)
        assertEquals("https://example.com/post", result.normalizedUrl)
    }

    @Test
    fun `rejects URLs with credentials`() {
        assertFailsWith<UnsupportedMemoryUrlException> {
            parser.parse("https://user:password@example.com/private")
        }
    }
}
