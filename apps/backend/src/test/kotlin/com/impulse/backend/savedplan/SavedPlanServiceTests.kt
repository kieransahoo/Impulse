package com.impulse.backend.savedplan

import com.impulse.backend.memory.Memory
import com.impulse.backend.memory.MemoryPlatform
import com.impulse.backend.memory.MemoryRepository
import com.impulse.backend.planning.CreatePlanResponse
import com.impulse.backend.planning.GroundingStatus
import com.impulse.backend.planning.PlanIntent
import com.impulse.backend.planning.PlanSourceType
import com.impulse.backend.planning.PlanStepResponse
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SavedPlanServiceTests {
    @Test
    fun `saves ordered plan steps and owned memory citations`() {
        val planRepository = mock(SavedPlanRepository::class.java)
        val stepRepository = mock(SavedPlanStepRepository::class.java)
        val memoryRepository = mock(MemoryRepository::class.java)
        val userId = UUID.randomUUID()
        val memory = memory(userId)
        val persistedPlan = SavedPlan(
            userId = userId,
            goal = "Focused morning",
            explanation = "Uses a saved deep-work routine.",
            retrievedMemoryIds = memory.id.toString(),
        )
        val savedSteps = mutableListOf<SavedPlanStep>()
        `when`(memoryRepository.findAllById(setOf(memory.id))).thenReturn(listOf(memory))
        `when`(planRepository.save(any(SavedPlan::class.java))).thenReturn(persistedPlan)
        `when`(stepRepository.save(any(SavedPlanStep::class.java))).thenAnswer {
            (it.arguments[0] as SavedPlanStep).also(savedSteps::add)
        }
        `when`(stepRepository.findAllByPlanIdOrderByOrderAsc(persistedPlan.id))
            .thenAnswer { savedSteps.sortedBy(SavedPlanStep::order) }

        val response = SavedPlanService(planRepository, stepRepository, memoryRepository).save(
            SavePlanRequest(
                userId = userId,
                goal = "Focused morning",
                explanation = "Uses a saved deep-work routine.",
                plan = listOf(
                    SavePlanStepRequest(
                        step = "Start a focus block",
                        durationMinutes = 60,
                        reason = "Matches the saved routine",
                        memoryIds = listOf(memory.id),
                    ),
                    SavePlanStepRequest(step = "Review the result"),
                ),
                retrievedMemoryIds = listOf(memory.id),
            ),
        )

        assertEquals(listOf(0, 1), response.plan.map(SavedPlanStepResponse::order))
        assertEquals(listOf(memory.id), response.plan.first().memoryIds)
        assertEquals(listOf(memory.id), response.retrievedMemoryIds)
    }

    @Test
    fun `rejects citations owned by another user`() {
        val planRepository = mock(SavedPlanRepository::class.java)
        val stepRepository = mock(SavedPlanStepRepository::class.java)
        val memoryRepository = mock(MemoryRepository::class.java)
        val userId = UUID.randomUUID()
        val foreignMemory = memory(UUID.randomUUID())
        `when`(memoryRepository.findAllById(setOf(foreignMemory.id)))
            .thenReturn(listOf(foreignMemory))

        assertFailsWith<InvalidPlanMemoryException> {
            SavedPlanService(planRepository, stepRepository, memoryRepository).save(
                SavePlanRequest(
                    userId = userId,
                    goal = "Invalid plan",
                    explanation = "Should fail.",
                    plan = listOf(
                        SavePlanStepRequest(
                            step = "Use foreign memory",
                            memoryIds = listOf(foreignMemory.id),
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun `activates plan persists step progress and marks it completed`() {
        val planRepository = mock(SavedPlanRepository::class.java)
        val stepRepository = mock(SavedPlanStepRepository::class.java)
        val memoryRepository = mock(MemoryRepository::class.java)
        val userId = UUID.randomUUID()
        val plan = SavedPlan(
            userId = userId,
            goal = "Morning routine",
            explanation = "A focused routine.",
            retrievedMemoryIds = null,
        )
        val step = SavedPlanStep(
            planId = plan.id,
            order = 0,
            step = "Complete the routine",
            durationMinutes = 20,
            reason = null,
            memoryIds = null,
        )
        `when`(planRepository.findByIdAndUserId(plan.id, userId)).thenReturn(plan)
        `when`(planRepository.save(plan)).thenReturn(plan)
        `when`(stepRepository.findByIdAndPlanId(step.id, plan.id)).thenReturn(step)
        `when`(stepRepository.findAllByPlanIdOrderByOrderAsc(plan.id)).thenReturn(listOf(step))
        `when`(stepRepository.save(step)).thenReturn(step)
        val service = SavedPlanService(planRepository, stepRepository, memoryRepository)

        assertEquals(SavedPlanStatus.ACTIVE, service.activate(plan.id, userId).status)
        val progressed = service.updateStep(
            plan.id,
            step.id,
            userId,
            UpdateStepCompletionRequest(completed = true),
        )
        assertEquals(true, progressed.plan.single().completed)
        assertEquals(SavedPlanStatus.ACTIVE, progressed.status)

        val completed = service.complete(plan.id, userId)
        assertEquals(SavedPlanStatus.COMPLETED, completed.status)
        assertEquals(true, completed.completedAt != null)
    }

    @Test
    fun `deletes owned plan and its steps without deleting memories`() {
        val planRepository = mock(SavedPlanRepository::class.java)
        val stepRepository = mock(SavedPlanStepRepository::class.java)
        val memoryRepository = mock(MemoryRepository::class.java)
        val userId = UUID.randomUUID()
        val plan = SavedPlan(
            userId = userId,
            goal = "Disposable plan",
            explanation = "Delete only this plan.",
            retrievedMemoryIds = null,
        )
        `when`(planRepository.findByIdAndUserId(plan.id, userId)).thenReturn(plan)

        SavedPlanService(planRepository, stepRepository, memoryRepository)
            .delete(plan.id, userId)

        verify(stepRepository).deleteAllByPlanId(plan.id)
        verify(planRepository).delete(plan)
    }

    @Test
    fun `returns the most recently activated plan for the user`() {
        val planRepository = mock(SavedPlanRepository::class.java)
        val stepRepository = mock(SavedPlanStepRepository::class.java)
        val memoryRepository = mock(MemoryRepository::class.java)
        val userId = UUID.randomUUID()
        val plan = SavedPlan(
            userId = userId,
            goal = "Current plan",
            explanation = "Continue this plan from Home.",
            retrievedMemoryIds = null,
            status = SavedPlanStatus.ACTIVE,
        )
        `when`(
            planRepository.findFirstByUserIdAndStatusOrderByActivatedAtDesc(
                userId,
                SavedPlanStatus.ACTIVE,
            ),
        ).thenReturn(plan)
        `when`(stepRepository.findAllByPlanIdOrderByOrderAsc(plan.id))
            .thenReturn(emptyList())

        val active = SavedPlanService(planRepository, stepRepository, memoryRepository)
            .findActive(userId)

        assertEquals(plan.id, active?.id)
        assertEquals(SavedPlanStatus.ACTIVE, active?.status)
    }

    @Test
    fun `rejects progress changes before a saved plan is started`() {
        val planRepository = mock(SavedPlanRepository::class.java)
        val stepRepository = mock(SavedPlanStepRepository::class.java)
        val memoryRepository = mock(MemoryRepository::class.java)
        val userId = UUID.randomUUID()
        val plan = SavedPlan(
            userId = userId,
            goal = "Not started",
            explanation = "Progress must stay locked.",
            retrievedMemoryIds = null,
        )
        `when`(planRepository.findByIdAndUserId(plan.id, userId)).thenReturn(plan)

        assertFailsWith<InvalidPlanTransitionException> {
            SavedPlanService(planRepository, stepRepository, memoryRepository).updateStep(
                plan.id,
                UUID.randomUUID(),
                userId,
                UpdateStepCompletionRequest(completed = true),
            )
        }
    }

    @Test
    fun `editing with latest memories replaces the original plan instead of duplicating it`() {
        val planRepository = mock(SavedPlanRepository::class.java)
        val stepRepository = mock(SavedPlanStepRepository::class.java)
        val memoryRepository = mock(MemoryRepository::class.java)
        val userId = UUID.randomUUID()
        val plan = SavedPlan(
            userId = userId,
            goal = "Old goal",
            explanation = "Old explanation",
            retrievedMemoryIds = null,
            status = SavedPlanStatus.ACTIVE,
        )
        val persistedSteps = mutableListOf<SavedPlanStep>()
        `when`(planRepository.findByIdAndUserId(plan.id, userId)).thenReturn(plan)
        `when`(planRepository.save(plan)).thenReturn(plan)
        `when`(stepRepository.save(any(SavedPlanStep::class.java))).thenAnswer {
            (it.arguments[0] as SavedPlanStep).also(persistedSteps::add)
        }
        `when`(stepRepository.findAllByPlanIdOrderByOrderAsc(plan.id))
            .thenAnswer { persistedSteps.sortedBy(SavedPlanStep::order) }
        doAnswer {
            persistedSteps.clear()
            null
        }.`when`(stepRepository).deleteAllByPlanId(plan.id)
        val generated = CreatePlanResponse(
            intent = PlanIntent.ROUTINE,
            groundingStatus = GroundingStatus.PARTIAL_GROUNDING,
            goal = "Updated goal",
            explanation = "Updated explanation",
            plan = listOf(
                PlanStepResponse(
                    step = "Updated first task",
                    durationMinutes = 15,
                    reason = "Uses current memories",
                    memoryIds = emptyList(),
                    sourceType = PlanSourceType.GENERAL,
                ),
            ),
            retrievedMemoryIds = emptyList(),
            groundingMemories = emptyList(),
        )

        val updated = SavedPlanService(planRepository, stepRepository, memoryRepository)
            .replace(plan.id, userId, generated)

        assertEquals(plan.id, updated.id)
        assertEquals("Updated goal", updated.goal)
        assertEquals(listOf("Updated first task"), updated.plan.map(SavedPlanStepResponse::step))
        assertEquals(SavedPlanStatus.SAVED, updated.status)
        verify(stepRepository).deleteAllByPlanId(plan.id)
        verify(planRepository).save(plan)
    }

    private fun memory(userId: UUID) = Memory(
        userId = userId,
        sourceUrl = "https://example.com/${UUID.randomUUID()}",
        platform = MemoryPlatform.WEB,
        title = "Deep work",
        description = null,
        summary = "Use focused blocks.",
        category = "productivity",
        userNote = null,
        embedding = FloatArray(768),
    )
}
