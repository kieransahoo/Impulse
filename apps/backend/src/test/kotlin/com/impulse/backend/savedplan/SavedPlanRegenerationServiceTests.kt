package com.impulse.backend.savedplan

import com.impulse.backend.planning.CreatePlanRequest
import com.impulse.backend.planning.CreatePlanResponse
import com.impulse.backend.planning.GroundingStatus
import com.impulse.backend.planning.PlanIntent
import com.impulse.backend.planning.PlanningService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class SavedPlanRegenerationServiceTests {
    @Test
    fun `regenerates saved goal against the current memory store`() {
        val savedPlans = mock(SavedPlanService::class.java)
        val planning = mock(PlanningService::class.java)
        val userId = UUID.randomUUID()
        val planId = UUID.randomUUID()
        val saved = SavedPlanResponse(
            id = planId,
            userId = userId,
            goal = "Create a three-day workout plan",
            explanation = "Old version",
            plan = emptyList(),
            retrievedMemoryIds = emptyList(),
            createdAt = Instant.now(),
        )
        val refreshed = CreatePlanResponse(
            intent = PlanIntent.WORKOUT,
            groundingStatus = GroundingStatus.STRONG_GROUNDING,
            goal = saved.goal,
            explanation = "Uses the latest memories",
            plan = emptyList(),
            retrievedMemoryIds = emptyList(),
            groundingMemories = emptyList(),
        )
        val editedGoal = "Create a four-day workout plan"
        val expectedRequest = CreatePlanRequest(
            userId = userId,
            query = editedGoal,
            constraints = mapOf("pace" to "Flexible"),
        )
        val replaced = saved.copyForTest(goal = editedGoal, explanation = refreshed.explanation)
        `when`(savedPlans.findOne(planId, userId)).thenReturn(saved)
        `when`(planning.createPlan(expectedRequest)).thenReturn(refreshed)
        `when`(savedPlans.replace(planId, userId, refreshed)).thenReturn(replaced)

        val result = SavedPlanRegenerationService(savedPlans, planning).regenerate(
            planId,
            RegeneratePlanRequest(
                userId,
                constraints = mapOf("pace" to "Flexible"),
                query = editedGoal,
            ),
        )

        verify(planning).createPlan(expectedRequest)
        verify(savedPlans).replace(planId, userId, refreshed)
        assertEquals(replaced, result)
    }

    private fun SavedPlanResponse.copyForTest(
        goal: String,
        explanation: String,
    ) = SavedPlanResponse(
        id = id,
        userId = userId,
        goal = goal,
        explanation = explanation,
        plan = plan,
        retrievedMemoryIds = retrievedMemoryIds,
        createdAt = createdAt,
    )
}
