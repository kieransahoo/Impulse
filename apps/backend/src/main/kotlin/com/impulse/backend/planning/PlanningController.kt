package com.impulse.backend.planning

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/impulse")
class PlanningController(
    private val service: PlanningService,
) {
    @PostMapping("/plan")
    fun createPlan(@Valid @RequestBody request: CreatePlanRequest): CreatePlanResponse =
        service.createPlan(request)
}
