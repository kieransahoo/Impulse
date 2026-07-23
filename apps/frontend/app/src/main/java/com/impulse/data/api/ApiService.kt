package com.impulse.data.api

import com.impulse.data.model.ShareRequest
import com.impulse.data.model.ShareResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit interface for YOUR backend.
 * Base URL is set in BuildConfig.BACKEND_BASE_URL.
 */
interface ApiService {

    /**
     * Sends a shared URL to the backend for processing / saving.
     *
     * @param token  "Bearer <google_id_token>"
     * @param request  URL payload + optional userId
     */
    @POST("share/url")
    suspend fun shareUrl(
        @Header("Authorization") token: String,
        @Body request: ShareRequest
    ): ShareResponse
}
