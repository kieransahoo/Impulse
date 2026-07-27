package com.impulse.backend.savedplan

import com.impulse.backend.memory.MemoryRepository
import com.impulse.backend.planning.CreatePlanResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

class SavedPlanNotFoundException(id: UUID) :
    NoSuchElementException("Saved plan $id was not found")

class InvalidPlanMemoryException :
    IllegalArgumentException("One or more cited memories do not belong to this user")

class SavedPlanStepNotFoundException(id: UUID) :
    NoSuchElementException("Saved plan step $id was not found")

class IncompletePlanException :
    IllegalStateException("Complete every step before marking the plan done")

class CannotReplacePlanException :
    IllegalStateException("No updated plan was generated, so the saved plan was not changed")

class InvalidPlanTransitionException(message: String) : IllegalStateException(message)

@Service
class SavedPlanService(
    private val plans: SavedPlanRepository,
    private val steps: SavedPlanStepRepository,
    private val memories: MemoryRepository,
) {
    @Transactional
    fun save(request: SavePlanRequest): SavedPlanResponse {
        validateMemoryOwnership(request)
        val plan = plans.save(
            SavedPlan(
                userId = request.userId,
                goal = request.goal.trim(),
                explanation = request.explanation.trim(),
                retrievedMemoryIds = encode(request.retrievedMemoryIds),
            ),
        )
        request.plan.forEachIndexed { index, input ->
            steps.save(
                SavedPlanStep(
                    planId = plan.id,
                    order = index,
                    step = input.step.trim(),
                    durationMinutes = input.durationMinutes,
                    reason = input.reason?.trim()?.takeIf(String::isNotBlank),
                    memoryIds = encode(input.memoryIds),
                ),
            )
        }
        return response(plan)
    }

    @Transactional(readOnly = true)
    fun findAll(userId: UUID): List<SavedPlanResponse> =
        plans.findAllByUserIdOrderByCreatedAtDesc(userId).map(::response)

    @Transactional(readOnly = true)
    fun findOne(id: UUID, userId: UUID): SavedPlanResponse =
        plans.findByIdAndUserId(id, userId)?.let(::response)
            ?: throw SavedPlanNotFoundException(id)

    @Transactional(readOnly = true)
    fun findActive(userId: UUID): SavedPlanResponse? =
        plans.findFirstByUserIdAndStatusOrderByActivatedAtDesc(
            userId,
            SavedPlanStatus.ACTIVE,
        )?.let(::response)

    @Transactional
    fun activate(id: UUID, userId: UUID): SavedPlanResponse {
        val plan = ownedPlan(id, userId)
        if (plan.status != SavedPlanStatus.COMPLETED) {
            plan.status = SavedPlanStatus.ACTIVE
            if (plan.activatedAt == null) plan.activatedAt = Instant.now()
            plans.save(plan)
        }
        return response(plan)
    }

    @Transactional
    fun updateStep(
        id: UUID,
        stepId: UUID,
        userId: UUID,
        request: UpdateStepCompletionRequest,
    ): SavedPlanResponse {
        val plan = ownedPlan(id, userId)
        if (plan.status != SavedPlanStatus.ACTIVE) {
            throw InvalidPlanTransitionException("Only an active plan can update task progress")
        }
        val step = steps.findByIdAndPlanId(stepId, id)
            ?: throw SavedPlanStepNotFoundException(stepId)
        step.completed = request.completed
        step.completedAt = if (request.completed) Instant.now() else null
        steps.save(step)
        plan.completedAt = null
        plans.save(plan)
        return response(plan)
    }

    @Transactional
    fun complete(id: UUID, userId: UUID): SavedPlanResponse {
        val plan = ownedPlan(id, userId)
        if (plan.status != SavedPlanStatus.ACTIVE) {
            throw InvalidPlanTransitionException("Only an active plan can be completed")
        }
        val allCompleted = steps.findAllByPlanIdOrderByOrderAsc(id)
            .all { it.completed == true }
        if (!allCompleted) throw IncompletePlanException()
        plan.status = SavedPlanStatus.COMPLETED
        plan.completedAt = plan.completedAt ?: Instant.now()
        plan.activatedAt = plan.activatedAt ?: plan.completedAt
        plans.save(plan)
        return response(plan)
    }

    @Transactional
    fun replace(id: UUID, userId: UUID, generated: CreatePlanResponse): SavedPlanResponse {
        if (generated.plan.isEmpty()) throw CannotReplacePlanException()
        val plan = ownedPlan(id, userId)
        val replacement = SavePlanRequest(
            userId = userId,
            goal = generated.goal,
            explanation = generated.explanation,
            plan = generated.plan.map {
                SavePlanStepRequest(
                    step = it.step,
                    durationMinutes = it.durationMinutes,
                    reason = it.reason,
                    memoryIds = it.memoryIds,
                )
            },
            retrievedMemoryIds = generated.retrievedMemoryIds,
        )
        validateMemoryOwnership(replacement)
        plan.goal = replacement.goal.trim()
        plan.explanation = replacement.explanation.trim()
        plan.retrievedMemoryIds = encode(replacement.retrievedMemoryIds)
        plan.status = SavedPlanStatus.SAVED
        plan.activatedAt = null
        plan.completedAt = null
        steps.deleteAllByPlanId(id)
        replacement.plan.forEachIndexed { index, input ->
            steps.save(
                SavedPlanStep(
                    planId = id,
                    order = index,
                    step = input.step.trim(),
                    durationMinutes = input.durationMinutes,
                    reason = input.reason?.trim()?.takeIf(String::isNotBlank),
                    memoryIds = encode(input.memoryIds),
                ),
            )
        }
        plans.save(plan)
        return response(plan)
    }

    @Transactional
    fun delete(id: UUID, userId: UUID) {
        val plan = ownedPlan(id, userId)
        steps.deleteAllByPlanId(id)
        plans.delete(plan)
    }

    private fun validateMemoryOwnership(request: SavePlanRequest) {
        val citedIds = (
            request.retrievedMemoryIds +
                request.plan.flatMap(SavePlanStepRequest::memoryIds)
            ).toSet()
        if (citedIds.isEmpty()) return
        val ownedIds = memories.findAllById(citedIds)
            .filter { it.userId == request.userId }
            .mapTo(mutableSetOf()) { it.id }
        if (ownedIds != citedIds) throw InvalidPlanMemoryException()
    }

    private fun response(plan: SavedPlan): SavedPlanResponse =
        SavedPlanResponse(
            id = plan.id,
            userId = plan.userId,
            goal = plan.goal,
            explanation = plan.explanation,
            plan = steps.findAllByPlanIdOrderByOrderAsc(plan.id).map {
                SavedPlanStepResponse(
                    id = it.id,
                    order = it.order,
                    step = it.step,
                    durationMinutes = it.durationMinutes,
                    reason = it.reason,
                    memoryIds = decode(it.memoryIds),
                    completed = it.completed == true,
                    completedAt = it.completedAt,
                )
            },
            retrievedMemoryIds = decode(plan.retrievedMemoryIds),
            createdAt = plan.createdAt,
            status = plan.status ?: SavedPlanStatus.SAVED,
            activatedAt = plan.activatedAt,
            completedAt = plan.completedAt,
        )

    private fun ownedPlan(id: UUID, userId: UUID): SavedPlan =
        plans.findByIdAndUserId(id, userId) ?: throw SavedPlanNotFoundException(id)

    private fun encode(ids: Collection<UUID>): String? =
        ids.distinct().joinToString("|").ifBlank { null }

    private fun decode(value: String?): List<UUID> =
        value?.split("|")?.mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: emptyList()
}
