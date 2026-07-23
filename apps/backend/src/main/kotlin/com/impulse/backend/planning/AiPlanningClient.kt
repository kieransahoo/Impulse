package com.impulse.backend.planning

import com.impulse.backend.memory.AiProcessingException
import com.impulse.backend.memory.AiMemoryProperties
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import tools.jackson.databind.ObjectMapper

interface AiPlanningClient {
    fun embedQuery(query: String): FloatArray
    fun createPlan(request: AiPlanRequest): AiPlanResponse
}

@Component
class HttpAiPlanningClient(
    properties: AiMemoryProperties,
    private val objectMapper: ObjectMapper,
) : AiPlanningClient {
    private val client = RestClient.builder()
        .baseUrl(properties.baseUrl)
        .requestFactory(SimpleClientHttpRequestFactory())
        .build()

    override fun embedQuery(query: String): FloatArray =
        call {
            client.post()
                .uri("/api/v1/embeddings/query")
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(AiQueryEmbeddingRequest(query)))
                .retrieve()
                .body(AiQueryEmbeddingResponse::class.java)
                ?.embedding
                ?.also {
                    if (it.size != EMBEDDING_DIMENSIONS) {
                        throw AiProcessingException("AI processor returned an invalid query embedding")
                    }
                }
                ?.toFloatArray()
                ?: throw AiProcessingException("AI processor returned an empty query embedding")
        }

    override fun createPlan(request: AiPlanRequest): AiPlanResponse =
        call {
            client.post()
                .uri("/api/v1/plans")
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(request))
                .retrieve()
                .body(AiPlanResponse::class.java)
                ?: throw AiProcessingException("AI processor returned an empty plan")
        }

    private fun <T> call(block: () -> T): T =
        try {
            block()
        } catch (exception: AiProcessingException) {
            throw exception
        } catch (exception: RestClientResponseException) {
            val detail = exception.responseBodyAsString
                .takeIf(String::isNotBlank)
                ?.take(1_000)
                ?: "AI processor rejected the request"
            throw AiProcessingException(
                "AI processor returned HTTP ${exception.statusCode.value()}: $detail",
                exception,
            )
        } catch (exception: RestClientException) {
            throw AiProcessingException("AI processor is unavailable", exception)
        }

    private companion object {
        const val EMBEDDING_DIMENSIONS = 768
    }
}
