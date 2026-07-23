package com.impulse.data.repository

import com.impulse.data.api.RetrofitClient
import com.impulse.data.model.CreateCollectionRequest
import com.impulse.data.model.SharedSourceRequest

class ShareRepository {
    private val api = RetrofitClient.apiService

    suspend fun collections(userId: String) = runCatching {
        api.getCollections(userId)
    }

    suspend fun save(
        userId: String,
        url: String,
        collectionName: String,
        note: String?
    ) = runCatching {
        api.createCollection(
            CreateCollectionRequest(
                userId = userId,
                name = collectionName,
                sources = listOf(SharedSourceRequest(url = url, userNote = note))
            )
        )
    }
}
