package com.impulse.backend.memory

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/memories")
class MemoryController(
    private val service: MemoryService,
    private val deletionService: MemoryDeletionService,
) {
    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    fun import(@Valid @RequestBody request: ImportMemoryRequest): MemoryResponse =
        service.import(request)

    @GetMapping
    fun findAll(@RequestParam userId: UUID): List<MemoryResponse> =
        service.findAll(userId)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable id: UUID,
        @RequestParam userId: UUID,
    ) = deletionService.delete(id, userId)

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun clear(@RequestParam userId: UUID) =
        deletionService.clear(userId)
}
