package com.impulse.backend.usercollection

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/collections")
class UserCollectionController(
    private val service: UserCollectionService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateUserCollectionRequest): UserCollectionResponse =
        service.create(request)

    @PostMapping("/sources")
    @ResponseStatus(HttpStatus.CREATED)
    fun addSource(@Valid @RequestBody request: AddSourceToCollectionRequest): UserCollectionResponse =
        service.addSource(request)

    @GetMapping
    fun findAll(@RequestParam userId: UUID): List<UserCollectionResponse> =
        service.findAll(userId)

    @GetMapping("/{id}")
    fun findOne(
        @PathVariable id: UUID,
        @RequestParam userId: UUID,
    ): UserCollectionResponse = service.findOne(id, userId)
}
