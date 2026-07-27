package com.impulse.backend.savedplan

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/plans")
class SavedPlanController(
    private val service: SavedPlanService,
    private val regeneration: SavedPlanRegenerationService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun save(@Valid @RequestBody request: SavePlanRequest): SavedPlanResponse =
        service.save(request)

    @GetMapping
    fun findAll(@RequestParam userId: UUID): List<SavedPlanResponse> =
        service.findAll(userId)

    @GetMapping("/active")
    fun findActive(@RequestParam userId: UUID): SavedPlanResponse? =
        service.findActive(userId)

    @GetMapping("/{id}")
    fun findOne(
        @PathVariable id: UUID,
        @RequestParam userId: UUID,
    ): SavedPlanResponse = service.findOne(id, userId)

    @PostMapping("/{id}/regenerate")
    fun regenerate(
        @PathVariable id: UUID,
        @Valid @RequestBody request: RegeneratePlanRequest,
    ) = regeneration.regenerate(id, request)

    @PatchMapping("/{id}/activate")
    fun activate(
        @PathVariable id: UUID,
        @RequestParam userId: UUID,
    ): SavedPlanResponse = service.activate(id, userId)

    @PatchMapping("/{id}/steps/{stepId}")
    fun updateStep(
        @PathVariable id: UUID,
        @PathVariable stepId: UUID,
        @RequestParam userId: UUID,
        @RequestBody request: UpdateStepCompletionRequest,
    ): SavedPlanResponse = service.updateStep(id, stepId, userId, request)

    @PatchMapping("/{id}/complete")
    fun complete(
        @PathVariable id: UUID,
        @RequestParam userId: UUID,
    ): SavedPlanResponse = service.complete(id, userId)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable id: UUID,
        @RequestParam userId: UUID,
    ) = service.delete(id, userId)
}
