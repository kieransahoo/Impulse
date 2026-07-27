package com.impulse.backend.memory

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import tools.jackson.databind.ObjectMapper
import java.time.Duration

data class AiMemoryRequest(
    val sourceUrl: String,
    val platform: MemoryPlatform,
    val userNote: String?,
    val content: String?,
)

data class AiMemoryResult(
    val title: String,
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val summary: String,
    val category: String,
    val tags: Set<String> = emptySet(),
    val topics: Set<String> = emptySet(),
    val actions: List<AiMemoryAction> = emptyList(),
    val embedding: List<Float>,
)

data class AiMemoryAction(
    val action: String,
    val useWhen: List<String> = emptyList(),
    val durationMinutes: Int? = null,
    val category: String? = null,
)

interface AiMemoryProcessor {
    fun process(request: AiMemoryRequest): AiMemoryResult
}

@ConfigurationProperties("impulse.ai")
data class AiMemoryProperties(
    var baseUrl: String = "http://localhost:8001",
    var memoryPath: String = "/api/v1/memories/process",
)

@Component
class HttpAiMemoryProcessor(
    private val properties: AiMemoryProperties,
    private val objectMapper: ObjectMapper,
) : AiMemoryProcessor {
    private val client = RestClient.builder()
        .baseUrl(properties.baseUrl)
        .requestFactory(
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(Duration.ofSeconds(5))
                setReadTimeout(Duration.ofSeconds(60))
            },
        )
        .build()

    override fun process(request: AiMemoryRequest): AiMemoryResult =
        try {
            client.post()
                .uri(properties.memoryPath)
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(request))
                .retrieve()
                .body(AiMemoryResult::class.java)
                ?.also(::validate)
                ?: throw AiProcessingException("AI processor returned an empty response")
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

    private fun validate(result: AiMemoryResult) {
        if (
            result.title.isBlank() ||
            result.summary.isBlank() ||
            result.category.isBlank() ||
            result.embedding.size != EMBEDDING_DIMENSIONS
        ) {
            throw AiProcessingException("AI processor returned incomplete memory data")
        }
    }

    private companion object {
        const val EMBEDDING_DIMENSIONS = 768
    }
}

class AiProcessingException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
