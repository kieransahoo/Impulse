package com.impulse.backend.collection

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import com.impulse.backend.memory.MemoryResponse
import java.util.UUID

@RestController
@RequestMapping("/api/link-collections")
class LinkCollectionController(
    private val service: LinkCollectionService,
    private val processingService: LinkCollectionProcessingService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun collect(@Valid @RequestBody request: CollectLinksRequest): LinkCollectionResponse =
        service.collect(request)

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    fun importCollected(
        @Valid @RequestBody request: ImportCollectedPlaylistRequest,
    ): LinkCollectionResponse = service.importCollected(request)

    @PostMapping("/{id}/process")
    fun process(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ProcessLinkCollectionRequest,
    ): MemoryResponse = processingService.process(id, request.userId, request.userNote)
}
