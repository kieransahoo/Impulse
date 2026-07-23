package com.impulse.data.api

import com.impulse.data.model.CreateCollectionRequest
import com.impulse.data.model.CreatePlanRequest
import com.impulse.data.model.MemoryResponse
import com.impulse.data.model.PlanResponse
import com.impulse.data.model.SearchMemoriesRequest
import com.impulse.data.model.UserCollectionResponse
import com.impulse.data.model.AuthResponse
import com.impulse.data.model.LoginRequest
import com.impulse.data.model.RegisterRequest
import com.impulse.data.model.AddSourceToCollectionRequest
import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit interface for YOUR backend.
 * Base URL is set in BuildConfig.BACKEND_BASE_URL.
 */
interface ApiService {
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/auth/logout")
    suspend fun logout(@Header("Authorization") authorization: String): Response<Unit>

    /**
     * Sends a shared URL to the backend for processing / saving.
     *
     * @param token  "Bearer <google_id_token>"
     * @param request  URL payload + optional userId
     */
    @POST("api/collections")
    suspend fun createCollection(@Body request: CreateCollectionRequest): UserCollectionResponse

    @POST("api/collections/sources")
    suspend fun addSourceToCollection(
        @Body request: AddSourceToCollectionRequest
    ): UserCollectionResponse

    @GET("api/collections")
    suspend fun getCollections(@Query("userId") userId: String): List<UserCollectionResponse>

    @GET("api/memories")
    suspend fun getMemories(@Query("userId") userId: String): List<MemoryResponse>

    @POST("api/memories/search")
    suspend fun searchMemories(@Body request: SearchMemoriesRequest): List<MemoryResponse>

    @POST("api/impulse/plan")
    suspend fun createPlan(@Body request: CreatePlanRequest): PlanResponse
}
