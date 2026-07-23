package com.impulse.backend.collection

import com.impulse.backend.memory.MemoryPlatform
import com.impulse.backend.memory.MemorySourceParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LinkCollectionService(
    private val repository: LinkCollectionRepository,
    private val sourceParser: MemorySourceParser,
    private val youtubeCollector: YouTubePlaylistCollector,
) {
    @Transactional
    fun collect(request: CollectLinksRequest): LinkCollectionResponse {
        val source = sourceParser.parse(request.url)
        if (source.platform != MemoryPlatform.YOUTUBE_PLAYLIST) {
            throw UnsupportedCollectionSourceException()
        }
        if (repository.existsByUserIdAndSourceUrl(request.userId, source.normalizedUrl)) {
            throw DuplicateCollectionException()
        }

        val playlistId = sourceParser.youtubePlaylistId(source.normalizedUrl)
            ?: throw UnsupportedCollectionSourceException()
        val result = youtubeCollector.collect(playlistId)
        val collection = LinkCollection(
            userId = request.userId,
            sourceUrl = source.normalizedUrl,
            platform = source.platform,
            title = result.title,
            items = result.items.map {
                CollectedLink(
                    url = "https://www.youtube.com/watch?v=${it.videoId}",
                    title = it.title,
                    description = it.description,
                    thumbnailUrl = it.thumbnailUrl,
                    position = it.position,
                )
            }.toMutableList(),
        )

        return repository.save(collection).toResponse()
    }

    @Transactional
    fun importCollected(request: ImportCollectedPlaylistRequest): LinkCollectionResponse {
        val source = sourceParser.parse(request.url)
        if (source.platform != MemoryPlatform.YOUTUBE_PLAYLIST) {
            throw UnsupportedCollectionSourceException()
        }
        if (repository.existsByUserIdAndSourceUrl(request.userId, source.normalizedUrl)) {
            throw DuplicateCollectionException()
        }

        val collection = LinkCollection(
            userId = request.userId,
            sourceUrl = source.normalizedUrl,
            platform = source.platform,
            title = request.title.trim(),
            items = request.items
                .sortedBy(ImportCollectedLinkRequest::position)
                .map {
                    CollectedLink(
                        url = it.url,
                        title = it.title.trim(),
                        description = it.description?.trim(),
                        thumbnailUrl = it.thumbnailUrl,
                        position = it.position,
                    )
                }
                .toMutableList(),
        )
        return repository.save(collection).toResponse()
    }
}

class UnsupportedCollectionSourceException :
    IllegalArgumentException("Automatic collection currently supports YouTube playlists only")

class DuplicateCollectionException :
    IllegalStateException("This playlist has already been collected for the user")
