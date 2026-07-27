package com.impulse.data.api

import com.impulse.data.model.CreateCollectionRequest
import com.impulse.data.model.CreatePlanRequest
import com.impulse.data.model.MemoryResponse
import com.impulse.data.model.PlanResponse
import com.impulse.data.model.RegeneratePlanRequest
import com.impulse.data.model.SearchMemoriesRequest
import com.impulse.data.model.UserCollectionResponse
import com.impulse.data.model.AuthResponse
import com.impulse.data.model.LoginRequest
import com.impulse.data.model.RegisterRequest
import com.impulse.data.model.AddSourceToCollectionRequest
import com.impulse.data.model.UpdateCollectionRequest
import com.impulse.data.model.SavePlanRequest
import com.impulse.data.model.SavedPlanResponse
import com.impulse.data.model.UpdateStepCompletionRequest
import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.DELETE
import retrofit2.http.Path
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

    @PATCH("api/collections/{id}")
    suspend fun updateCollection(
        @Path("id") id: String,
        @Body request: UpdateCollectionRequest
    ): UserCollectionResponse

    @DELETE("api/collections/{id}")
    suspend fun deleteCollection(
        @Path("id") id: String,
        @Query("userId") userId: String
    ): Response<Unit>

    @DELETE("api/collections/{collectionId}/sources/{sourceId}")
    suspend fun removeCollectionSource(
        @Path("collectionId") collectionId: String,
        @Path("sourceId") sourceId: String,
        @Query("userId") userId: String
    ): Response<Unit>

    @GET("api/memories")
    suspend fun getMemories(@Query("userId") userId: String): List<MemoryResponse>

    @DELETE("api/memories/{id}")
    suspend fun deleteMemory(
        @Path("id") id: String,
        @Query("userId") userId: String
    ): Response<Unit>

    @DELETE("api/memories")
    suspend fun clearMemories(@Query("userId") userId: String): Response<Unit>

    @POST("api/memories/search")
    suspend fun searchMemories(@Body request: SearchMemoriesRequest): List<MemoryResponse>

    @POST("api/impulse/plan")
    suspend fun createPlan(@Body request: CreatePlanRequest): PlanResponse

    @POST("api/plans")
    suspend fun savePlan(@Body request: SavePlanRequest): SavedPlanResponse

    @GET("api/plans")
    suspend fun getPlans(@Query("userId") userId: String): List<SavedPlanResponse>

    @GET("api/plans/{id}")
    suspend fun getPlan(
        @Path("id") id: String,
        @Query("userId") userId: String
    ): SavedPlanResponse

    @POST("api/plans/{id}/regenerate")
    suspend fun regeneratePlan(
        @Path("id") id: String,
        @Body request: RegeneratePlanRequest
    ): SavedPlanResponse

    @PATCH("api/plans/{id}/activate")
    suspend fun activatePlan(
        @Path("id") id: String,
        @Query("userId") userId: String
    ): SavedPlanResponse

    @PATCH("api/plans/{id}/steps/{stepId}")
    suspend fun updatePlanStep(
        @Path("id") id: String,
        @Path("stepId") stepId: String,
        @Query("userId") userId: String,
        @Body request: UpdateStepCompletionRequest
    ): SavedPlanResponse

    @PATCH("api/plans/{id}/complete")
    suspend fun completePlan(
        @Path("id") id: String,
        @Query("userId") userId: String
    ): SavedPlanResponse

    @DELETE("api/plans/{id}")
    suspend fun deletePlan(
        @Path("id") id: String,
        @Query("userId") userId: String
    ): Response<Unit>
}
