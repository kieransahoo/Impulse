package com.impulse.data.repository

import com.impulse.data.api.RetrofitClient
import com.impulse.data.model.ShareRequest
import com.impulse.data.model.ShareResponse

/**
 * Repository for sending captured URLs to the backend.
 */
class ShareRepository {

    private val api = RetrofitClient.apiService

    /**
     * Posts the shared URL to the backend.
     *
     * @param url     The extracted URL string
     * @param userId  Logged-in user ID (optional — allows anonymous shares)
     * @param idToken Google ID token for Bearer auth
     */
    suspend fun sendUrl(
        url: String,
        userId: String?,
        idToken: String
    ): Result<ShareResponse> = runCatching {
        api.shareUrl(
            token   = "Bearer $idToken",
            request = ShareRequest(url = url, userId = userId)
        )
    }
}
