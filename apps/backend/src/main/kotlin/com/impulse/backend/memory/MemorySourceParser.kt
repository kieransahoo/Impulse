package com.impulse.backend.memory

import org.springframework.stereotype.Component
import java.net.URI

data class ParsedMemorySource(
    val normalizedUrl: String,
    val platform: MemoryPlatform,
)

@Component
class MemorySourceParser {
    fun parse(rawUrl: String): ParsedMemorySource {
        val uri = runCatching { URI(rawUrl.trim()) }
            .getOrElse { throw UnsupportedMemoryUrlException() }

        if (uri.scheme !in setOf("http", "https") || uri.host.isNullOrBlank()) {
            throw UnsupportedMemoryUrlException()
        }

        val host = uri.host.lowercase().removePrefix("www.")
        val pathParts = uri.path.split('/').filter(String::isNotBlank)

        val platform = when {
            host in YOUTUBE_HOSTS &&
                pathParts.firstOrNull() == "playlist" &&
                queryParameter(uri.rawQuery, "list").isNullOrBlank().not() ->
                MemoryPlatform.YOUTUBE_PLAYLIST

            host in YOUTUBE_HOSTS &&
                (
                    pathParts.firstOrNull() == "shorts" && pathParts.size >= 2 ||
                        pathParts.firstOrNull() == "watch" &&
                        queryParameter(uri.rawQuery, "v").isNullOrBlank().not()
                ) ->
                MemoryPlatform.YOUTUBE_VIDEO

            host == "youtu.be" && pathParts.isNotEmpty() ->
                MemoryPlatform.YOUTUBE_VIDEO

            host == "instagram.com" &&
                pathParts.size >= 2 &&
                pathParts.first() in INSTAGRAM_CONTENT_PATHS ->
                MemoryPlatform.INSTAGRAM

            else -> MemoryPlatform.WEB
        }

        if (!uri.userInfo.isNullOrBlank()) throw UnsupportedMemoryUrlException()
        val normalized = URI(
            uri.scheme.lowercase(),
            null,
            uri.host.lowercase(),
            uri.port,
            uri.path.ifBlank { "/" },
            uri.rawQuery,
            null,
        )
        return ParsedMemorySource(
            normalizedUrl = normalized.normalize().toASCIIString(),
            platform = platform,
        )
    }

    fun youtubePlaylistId(rawUrl: String): String? {
        val uri = runCatching { URI(rawUrl.trim()) }.getOrNull() ?: return null
        val host = uri.host?.lowercase()?.removePrefix("www.") ?: return null
        if (host !in YOUTUBE_HOSTS || uri.path != "/playlist") {
            return null
        }
        return queryParameter(uri.rawQuery, "list")?.takeIf(String::isNotBlank)
    }

    private fun queryParameter(query: String?, name: String): String? =
        query
            ?.split('&')
            ?.mapNotNull {
                val parts = it.split('=', limit = 2)
                parts.takeIf { values -> values.size == 2 }?.let { values -> values[0] to values[1] }
            }
            ?.firstOrNull { it.first == name }
            ?.second

    private companion object {
        val YOUTUBE_HOSTS = setOf("youtube.com", "music.youtube.com")
        val INSTAGRAM_CONTENT_PATHS = setOf("p", "reel", "tv")
    }
}

class UnsupportedMemoryUrlException :
    IllegalArgumentException("A valid public HTTP or HTTPS URL is required")
