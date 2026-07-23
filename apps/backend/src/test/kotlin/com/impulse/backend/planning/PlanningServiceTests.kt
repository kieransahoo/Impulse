package com.impulse.backend.planning

import com.impulse.backend.memory.Memory
import com.impulse.backend.memory.MemoryPlatform
import com.impulse.backend.memory.MemoryRepository
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PlanningServiceTests {
    @Test
    fun `retrieves only the requesting user's memories and returns cited plan`() {
        val repository = mock(MemoryRepository::class.java)
        val userId = UUID.randomUUID()
        val memory = Memory(
            userId = userId,
            sourceUrl = "https://www.youtube.com/watch?v=abc",
            platform = MemoryPlatform.YOUTUBE_VIDEO,
            title = "Deep work",
            description = null,
            summary = "Use focused work blocks.",
            category = "productivity",
            topics = mutableSetOf("deep work"),
            userNote = null,
            embedding = FloatArray(768) { 0.1f },
        )
        `when`(repository.findAllByUserIdOrderByCreatedAtDesc(userId)).thenReturn(listOf(memory))

        val aiClient = object : AiPlanningClient {
            override fun embedQuery(query: String) = FloatArray(768) { 0.1f }

            override fun createPlan(request: AiPlanRequest) = AiPlanResponse(
                goal = request.query,
                explanation = "Used a matching deep-work memory.",
                plan = listOf(
                    AiPlanStep(
                        step = "Start a focus block",
                        durationMinutes = 90,
                        reason = "Matches the saved technique",
                        memoryIds = listOf(memory.id.toString()),
                    ),
                ),
            )
        }

        val retrievalService = MemoryRetrievalService(repository, aiClient)
        val response = PlanningService(retrievalService, aiClient).createPlan(
            CreatePlanRequest(userId = userId, query = "Plan a deep work session"),
        )

        assertEquals(listOf(memory.id), response.retrievedMemoryIds)
        assertEquals(listOf(memory.id), response.plan.single().memoryIds)
    }

    @Test
    fun `retrieval ranks a keyword and semantic match ahead of other user memories`() {
        val repository = mock(MemoryRepository::class.java)
        val userId = UUID.randomUUID()
        val deepWork = memory(
            userId = userId,
            title = "Deep work routine",
            summary = "Schedule a focused coding block.",
            embeddingValue = 0.1f,
        )
        val cooking = memory(
            userId = userId,
            title = "Pasta recipe",
            summary = "Cook tomato pasta.",
            embeddingValue = -0.1f,
        )
        `when`(repository.findAllByUserIdOrderByCreatedAtDesc(userId))
            .thenReturn(listOf(cooking, deepWork))
        val aiClient = object : AiPlanningClient {
            override fun embedQuery(query: String) = FloatArray(768) { 0.1f }
            override fun createPlan(request: AiPlanRequest) = error("not used")
        }

        val results = MemoryRetrievalService(repository, aiClient)
            .retrieve(userId, "plan focused coding work")

        assertEquals(deepWork.id, results.first().memory.id)
        assertEquals(2, results.size)
    }

    private fun memory(
        userId: UUID,
        title: String,
        summary: String,
        embeddingValue: Float,
    ) = Memory(
        userId = userId,
        sourceUrl = "https://www.youtube.com/watch?v=${UUID.randomUUID()}",
        platform = MemoryPlatform.YOUTUBE_VIDEO,
        title = title,
        description = null,
        summary = summary,
        category = "test",
        userNote = null,
        embedding = FloatArray(768) { embeddingValue },
    )
}
