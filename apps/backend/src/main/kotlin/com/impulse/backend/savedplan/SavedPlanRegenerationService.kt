package com.impulse.backend.savedplan

import com.impulse.backend.planning.CreatePlanRequest
import com.impulse.backend.planning.PlanningService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SavedPlanRegenerationService(
    private val savedPlans: SavedPlanService,
    private val planning: PlanningService,
) {
    fun regenerate(id: UUID, request: RegeneratePlanRequest): SavedPlanResponse {
        val saved = savedPlans.findOne(id, request.userId)
        val goal = request.query?.trim()?.takeIf(String::isNotBlank) ?: saved.goal
        val generated = planning.createPlan(
            CreatePlanRequest(
                userId = request.userId,
                query = goal,
                constraints = request.constraints,
                allowGeneralKnowledge = request.allowGeneralKnowledge,
            ),
        )
        return savedPlans.replace(id, request.userId, generated)
    }
}
