package com.impulse.data.repository

import com.impulse.data.api.RetrofitClient
import com.impulse.data.model.CreateCollectionRequest
import com.impulse.data.model.CreatePlanRequest
import com.impulse.data.model.SearchMemoriesRequest
import com.impulse.data.model.SharedSourceRequest
import com.impulse.data.model.UpdateCollectionRequest
import com.impulse.data.model.AddSourceToCollectionRequest
import com.impulse.data.model.PlanResponse
import com.impulse.data.model.SavePlanRequest
import com.impulse.data.model.SavePlanStepRequest
import com.impulse.data.model.RegeneratePlanRequest
import com.impulse.data.model.UpdateStepCompletionRequest

class ImpulseRepository {
    private val api = RetrofitClient.apiService

    suspend fun loadWorkspace(userId: String) = runCatching {
        api.getCollections(userId) to api.getMemories(userId)
    }

    suspend fun createCollection(
        userId: String,
        name: String,
        description: String?,
        urls: List<String>
    ) = runCatching {
        api.createCollection(
            CreateCollectionRequest(
                userId = userId,
                name = name,
                description = description,
                sources = urls.map(::SharedSourceRequest)
            )
        )
    }

    suspend fun createCollectionWithSource(
        userId: String,
        name: String,
        url: String,
        note: String?
    ) = runCatching {
        api.createCollection(
            CreateCollectionRequest(
                userId = userId,
                name = name,
                description = null,
                sources = listOf(SharedSourceRequest(url, note))
            )
        )
    }

    suspend fun search(userId: String, query: String) = runCatching {
        api.searchMemories(SearchMemoriesRequest(userId, query))
    }

    suspend fun deleteMemory(userId: String, memoryId: String) =
        runCatching { api.deleteMemory(memoryId, userId) }

    suspend fun clearMemories(userId: String) =
        runCatching { api.clearMemories(userId) }

    suspend fun plan(
        userId: String,
        query: String,
        constraints: Map<String, Any?> = emptyMap(),
        allowGeneralKnowledge: Boolean = false
    ) = runCatching {
        api.createPlan(CreatePlanRequest(userId, query, constraints, allowGeneralKnowledge))
    }

    suspend fun plans(userId: String) = runCatching { api.getPlans(userId) }

    suspend fun regeneratePlan(
        userId: String,
        planId: String,
        constraints: Map<String, Any?> = emptyMap(),
        allowGeneralKnowledge: Boolean = false,
        query: String? = null
    ) = runCatching {
        api.regeneratePlan(
            planId,
            RegeneratePlanRequest(userId, constraints, allowGeneralKnowledge, query)
        )
    }

    suspend fun activatePlan(userId: String, planId: String) =
        runCatching { api.activatePlan(planId, userId) }

    suspend fun updatePlanStep(
        userId: String,
        planId: String,
        stepId: String,
        completed: Boolean
    ) = runCatching {
        api.updatePlanStep(planId, stepId, userId, UpdateStepCompletionRequest(completed))
    }

    suspend fun completePlan(userId: String, planId: String) =
        runCatching { api.completePlan(planId, userId) }

    suspend fun deletePlan(userId: String, planId: String) =
        runCatching {
            val response = api.deletePlan(planId, userId)
            check(response.isSuccessful) {
                "Could not delete plan (${response.code()})."
            }
        }

    suspend fun savePlan(userId: String, plan: PlanResponse) = runCatching {
        api.savePlan(
            SavePlanRequest(
                userId = userId,
                goal = plan.goal,
                explanation = plan.explanation,
                plan = plan.plan.map {
                    SavePlanStepRequest(it.step, it.durationMinutes, it.reason, it.memoryIds)
                },
                retrievedMemoryIds = plan.retrievedMemoryIds
            )
        )
    }

    suspend fun addSource(
        userId: String,
        collectionId: String?,
        url: String,
        note: String?
    ) = runCatching {
        api.addSourceToCollection(AddSourceToCollectionRequest(userId, collectionId, url, note))
    }

    suspend fun updateCollection(
        userId: String,
        collectionId: String,
        name: String,
        description: String?
    ) = runCatching {
        api.updateCollection(collectionId, UpdateCollectionRequest(userId, name, description))
    }

    suspend fun deleteCollection(userId: String, collectionId: String) = runCatching {
        api.deleteCollection(collectionId, userId)
    }

    suspend fun removeSource(userId: String, collectionId: String, sourceId: String) =
        runCatching { api.removeCollectionSource(collectionId, sourceId, userId) }
}
