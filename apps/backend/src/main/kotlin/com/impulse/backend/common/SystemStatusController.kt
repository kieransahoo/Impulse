package com.impulse.backend.common

import com.impulse.backend.memory.AiMemoryProperties
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClient
import org.springframework.http.client.SimpleClientHttpRequestFactory

data class SystemStatusResponse(
    val backend: String = "UP",
    val aiReady: Boolean,
)

@RestController
@RequestMapping("/api/system")
class SystemStatusController(
    properties: AiMemoryProperties,
) {
    private val aiClient = RestClient.builder()
        .baseUrl(properties.baseUrl)
        .requestFactory(SimpleClientHttpRequestFactory())
        .build()

    @GetMapping("/status")
    fun status(): SystemStatusResponse {
        val ready = runCatching {
            aiClient.get().uri("/ready").retrieve().toBodilessEntity().statusCode.is2xxSuccessful
        }.getOrDefault(false)
        return SystemStatusResponse(aiReady = ready)
    }
}
