package com.impulse.data.repository

import com.impulse.BuildConfig
import com.impulse.data.api.RetrofitClient
import com.impulse.data.model.ChatMessage
import com.impulse.data.model.ChatRequest

/**
 * Repository for interacting with the OpenAI Chat Completions API.
 *
 * Maintains no state — callers (ViewModel) pass the full message history.
 */
class ChatRepository {

    private val openAi = RetrofitClient.openAiService

    private val systemPrompt = ChatMessage(
        role    = "system",
        content = "You are a helpful, friendly, and concise AI assistant. " +
                  "Respond clearly and thoroughly, using markdown formatting when appropriate."
    )

    /**
     * Sends the full conversation history to OpenAI and returns the assistant's reply.
     *
     * @param messages  All messages exchanged so far (user + assistant turns)
     * @return          Result wrapping the assistant reply text
     */
    suspend fun sendMessage(messages: List<ChatMessage>): Result<String> = runCatching {
        val request = ChatRequest(
            messages = listOf(systemPrompt) + messages
        )
        val response = openAi.chatCompletion(
            bearerToken = "Bearer ${BuildConfig.OPENAI_API_KEY}",
            request     = request
        )
        response.choices.firstOrNull()?.message?.content
            ?: "I didn't receive a response. Please try again."
    }
}
