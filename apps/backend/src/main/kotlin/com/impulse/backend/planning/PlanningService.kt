package com.impulse.backend.planning

import com.impulse.backend.memory.Memory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PlanningService(
    private val retrievalService: MemoryRetrievalService,
    private val aiClient: AiPlanningClient,
) {
    fun createPlan(request: CreatePlanRequest): CreatePlanResponse {
        val retrieved = retrievalService.retrieve(request.userId, request.query)
        if (retrieved.isEmpty()) throw NoMemoriesForPlanningException()
        val memories = retrieved.map(RetrievedMemory::memory)

        val aiResponse = aiClient.createPlan(
            AiPlanRequest(
                query = request.query,
                constraints = request.constraints,
                memories = memories.map(::toPlanningMemory),
            ),
        )
        val retrievedIds = memories.map(Memory::id).toSet()
        return CreatePlanResponse(
            goal = aiResponse.goal,
            explanation = aiResponse.explanation,
            plan = aiResponse.plan.map { step ->
                PlanStepResponse(
                    step = step.step,
                    durationMinutes = step.durationMinutes,
                    reason = step.reason,
                    memoryIds = step.memoryIds
                        .mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }
                        .filter(retrievedIds::contains),
                )
            },
            retrievedMemoryIds = memories.map(Memory::id),
            groundingMemories = memories.map {
                GroundingMemoryResponse(
                    id = it.id,
                    title = it.title,
                    summary = it.summary,
                    sourceUrl = it.sourceUrl,
                    thumbnailUrl = it.thumbnailUrl,
                    platform = it.platform.name,
                )
            },
        )
    }

    private fun toPlanningMemory(memory: Memory) = AiPlanningMemory(
        id = memory.id.toString(),
        title = memory.title,
        summary = memory.summary,
        category = memory.category,
        topics = memory.topics,
        actions = memory.actions.map {
            AiPlanningAction(
                action = it.action,
                useWhen = it.useWhen?.split("|")?.filter(String::isNotBlank) ?: emptyList(),
                durationMinutes = it.durationMinutes,
                category = it.category,
            )
        },
        sourceUrl = memory.sourceUrl,
    )
}

class NoMemoriesForPlanningException :
    IllegalStateException("No saved memories are available for this user. Save content before creating a plan.")
