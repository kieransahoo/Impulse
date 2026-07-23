package com.impulse.backend.collection

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

data class CollectedPlaylist(
    val title: String,
    val items: List<CollectedPlaylistItem>,
)

data class CollectedPlaylistItem(
    val videoId: String,
    val title: String,
    val description: String?,
    val thumbnailUrl: String?,
    val position: Int,
)

interface YouTubePlaylistCollector {
    fun collect(playlistId: String): CollectedPlaylist
}

@ConfigurationProperties("impulse.youtube")
data class YouTubeProperties(
    var apiKey: String = "",
    var baseUrl: String = "https://www.googleapis.com/youtube/v3",
    var maxItems: Int = 200,
)

@Component
class HttpYouTubePlaylistCollector(
    private val properties: YouTubeProperties,
) : YouTubePlaylistCollector {
    private val client = RestClient.builder().baseUrl(properties.baseUrl).build()

    override fun collect(playlistId: String): CollectedPlaylist {
        if (properties.apiKey.isBlank()) {
            throw YouTubeConfigurationException()
        }

        val items = mutableListOf<CollectedPlaylistItem>()
        var pageToken: String? = null

        do {
            val response = fetchPage(playlistId, pageToken)
            items += response.items.mapNotNull { item ->
                val videoId = item.contentDetails?.videoId ?: item.snippet?.resourceId?.videoId
                val snippet = item.snippet
                if (videoId.isNullOrBlank() || snippet == null) {
                    null
                } else {
                    CollectedPlaylistItem(
                        videoId = videoId,
                        title = snippet.title.orEmpty(),
                        description = snippet.description,
                        thumbnailUrl = snippet.thumbnails?.bestUrl(),
                        position = snippet.position ?: items.size,
                    )
                }
            }.take(properties.maxItems - items.size)
            pageToken = response.nextPageToken
        } while (!pageToken.isNullOrBlank() && items.size < properties.maxItems)

        if (items.isEmpty()) {
            throw YouTubeCollectionException("Playlist was not found, is private, or contains no accessible videos")
        }

        return CollectedPlaylist(
            title = "YouTube playlist",
            items = items,
        )
    }

    private fun fetchPage(playlistId: String, pageToken: String?): YouTubePlaylistItemsResponse =
        try {
            client.get()
                .uri { builder ->
                    builder
                        .path("/playlistItems")
                        .queryParam("part", "snippet,contentDetails")
                        .queryParam("playlistId", playlistId)
                        .queryParam("maxResults", 50)
                        .queryParam("key", properties.apiKey)
                        .apply {
                            if (!pageToken.isNullOrBlank()) {
                                queryParam("pageToken", pageToken)
                            }
                        }
                        .build()
                }
                .retrieve()
                .onStatus(HttpStatusCode::isError) { _, response ->
                    throw YouTubeCollectionException(
                        "YouTube API returned ${response.statusCode.value()}",
                    )
                }
                .body(YouTubePlaylistItemsResponse::class.java)
                ?: throw YouTubeCollectionException("YouTube API returned an empty response")
        } catch (exception: YouTubeCollectionException) {
            throw exception
        } catch (exception: RestClientException) {
            throw YouTubeCollectionException("YouTube API is unavailable", exception)
        }
}

data class YouTubePlaylistItemsResponse(
    val nextPageToken: String? = null,
    val items: List<YouTubePlaylistItem> = emptyList(),
)

data class YouTubePlaylistItem(
    val snippet: YouTubeSnippet? = null,
    val contentDetails: YouTubeContentDetails? = null,
)

data class YouTubeSnippet(
    val title: String? = null,
    val description: String? = null,
    val position: Int? = null,
    val resourceId: YouTubeResourceId? = null,
    val thumbnails: Map<String, YouTubeThumbnail>? = null,
)

data class YouTubeContentDetails(val videoId: String? = null)
data class YouTubeResourceId(val videoId: String? = null)
data class YouTubeThumbnail(val url: String? = null, val width: Int? = null)

private fun Map<String, YouTubeThumbnail>.bestUrl(): String? =
    values.maxByOrNull { it.width ?: 0 }?.url

class YouTubeConfigurationException :
    IllegalStateException("YOUTUBE_API_KEY is required to collect YouTube playlists")

class YouTubeCollectionException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
