package com.impulse.data.api

import com.impulse.data.model.ChatRequest
import com.impulse.data.model.ChatResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit interface for the OpenAI Chat Completions API.
 * Base URL: https://api.openai.com/
 */
interface OpenAiService {

    /**
     * Sends a chat completion request.
     *
     * @param bearerToken  "Bearer sk-<your_openai_key>"
     * @param request      ChatRequest with model, messages, and parameters
     */
    @POST("v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") bearerToken: String,
        @Body request: ChatRequest
    ): ChatResponse
}
