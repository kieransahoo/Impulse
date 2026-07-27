package com.impulse.backend.planning

import com.impulse.backend.memory.Memory
import com.impulse.backend.usercollection.UserCollectionRepository
import com.impulse.backend.usercollection.UserCollectionSourceRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PlanningService(
    private val retrievalService: MemoryRetrievalService,
    private val aiClient: AiPlanningClient,
    private val preparationService: PlanningPreparationService,
    private val collectionRepository: UserCollectionRepository,
    private val collectionSourceRepository: UserCollectionSourceRepository,
) {
    fun createPlan(request: CreatePlanRequest): CreatePlanResponse {
        val preparation = preparationService.prepare(request.query, request.constraints)
        val allowedMemoryIds = selectedCollectionMemoryIds(request)
        val allowGeneralKnowledge = request.allowGeneralKnowledge && allowedMemoryIds == null
        val retrieved = retrievalService.retrieveRelevant(
            request.userId,
            request.query,
            intent = preparation.intent,
            allowedMemoryIds = allowedMemoryIds,
        ).filter { result ->
            allowedMemoryIds == null || result.memory.id in allowedMemoryIds
        }
        val memories = retrieved.map(RetrievedMemory::memory)
        val groundingStatus = when {
            memories.isEmpty() -> GroundingStatus.NO_GROUNDING
            memories.size >= 3 && retrieved.first().score >= STRONG_GROUNDING_SCORE ->
                GroundingStatus.STRONG_GROUNDING
            else -> GroundingStatus.PARTIAL_GROUNDING
        }

        if (memories.isEmpty() && !allowGeneralKnowledge) {
            val explanation = if (allowedMemoryIds != null) {
                "No relevant saved memories were found in the selected collections. Choose other collections or search all memories."
            } else {
                "No relevant saved memories were found. Add related sources or explicitly choose a general starter plan."
            }
            return CreatePlanResponse(
                intent = preparation.intent,
                groundingStatus = GroundingStatus.NO_GROUNDING,
                goal = request.query,
                explanation = explanation,
                plan = emptyList(),
                retrievedMemoryIds = emptyList(),
                groundingMemories = emptyList(),
                missingContext = preparation.missingContext,
                suggestedSources = preparation.suggestedSources,
            )
        }

        val aiResponse = aiClient.createPlan(
            AiPlanRequest(
                query = request.query,
                constraints = request.constraints,
                memories = memories.map(::toPlanningMemory),
                intent = preparation.intent,
                groundingStatus = groundingStatus,
                allowGeneralKnowledge = allowGeneralKnowledge,
                missingContext = preparation.missingContext,
            ),
        )
        val retrievedIds = memories.map(Memory::id).toSet()
        return CreatePlanResponse(
            intent = preparation.intent,
            groundingStatus = groundingStatus,
            goal = aiResponse.goal,
            explanation = aiResponse.explanation,
            plan = aiResponse.plan.mapNotNull { step ->
                val validIds = step.memoryIds
                    .mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }
                    .filter(retrievedIds::contains)
                if (validIds.isEmpty() && !allowGeneralKnowledge) {
                    return@mapNotNull null
                }
                PlanStepResponse(
                    step = step.step,
                    durationMinutes = step.durationMinutes,
                    reason = step.reason,
                    memoryIds = validIds,
                    sourceType = if (validIds.isNotEmpty()) {
                        PlanSourceType.MEMORY
                    } else {
                        PlanSourceType.GENERAL
                    },
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
            missingContext = preparation.missingContext,
            suggestedSources = preparation.suggestedSources,
        )
    }

    private fun selectedCollectionMemoryIds(request: CreatePlanRequest): Set<UUID>? {
        val rawIds = request.constraints["collectionIds"] as? Collection<*> ?: return null
        if (rawIds.isEmpty()) return null
        return rawIds
            .map {
                runCatching { UUID.fromString(it?.toString()) }
                    .getOrElse { throw InvalidPlanCollectionException("Invalid selected collection") }
            }
            .flatMap { collectionId ->
                collectionRepository.findByIdAndUserId(collectionId, request.userId)
                    ?: throw InvalidPlanCollectionException(
                        "Selected collection is not available for this user",
                    )
                collectionSourceRepository
                    .findAllByCollectionIdAndUserIdOrderByCreatedAtAsc(collectionId, request.userId)
                    .mapNotNull { it.memoryId }
            }
            .toSet()
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

    private companion object {
        const val STRONG_GROUNDING_SCORE = 0.45
    }
}

class NoMemoriesForPlanningException :
    IllegalStateException("No saved memories are available for this user. Save content before creating a plan.")

class InvalidPlanCollectionException(message: String) : IllegalArgumentException(message)
