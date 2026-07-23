package com.impulse.data.repository

import com.impulse.data.api.RetrofitClient
import com.impulse.data.model.CreateCollectionRequest
import com.impulse.data.model.CreatePlanRequest
import com.impulse.data.model.SearchMemoriesRequest
import com.impulse.data.model.SharedSourceRequest

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

    suspend fun search(userId: String, query: String) = runCatching {
        api.searchMemories(SearchMemoriesRequest(userId, query))
    }

    suspend fun plan(userId: String, query: String) = runCatching {
        api.createPlan(CreatePlanRequest(userId, query))
    }
}
