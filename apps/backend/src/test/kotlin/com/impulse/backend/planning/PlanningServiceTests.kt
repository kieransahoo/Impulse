package com.impulse.backend.planning

import com.impulse.backend.memory.Memory
import com.impulse.backend.memory.MemoryPlatform
import com.impulse.backend.memory.MemoryRepository
import com.impulse.backend.usercollection.UserCollectionRepository
import com.impulse.backend.usercollection.UserCollectionSourceRepository
import com.impulse.backend.usercollection.UserCollection
import com.impulse.backend.usercollection.UserCollectionSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PlanningServiceTests {
    @Test
    fun `returns an honest preparation response when no relevant memory exists`() {
        val repository = mock(MemoryRepository::class.java)
        val userId = UUID.randomUUID()
        `when`(repository.findAllByUserIdOrderByCreatedAtDesc(userId)).thenReturn(emptyList())
        val aiClient = object : AiPlanningClient {
            override fun embedQuery(query: String) = FloatArray(768)
            override fun createPlan(request: AiPlanRequest) =
                error("AI generation must not run without user consent")
        }

        val response = PlanningService(
            MemoryRetrievalService(repository, aiClient),
            aiClient,
            PlanningPreparationService(),
            mock(UserCollectionRepository::class.java),
            mock(UserCollectionSourceRepository::class.java),
        ).createPlan(CreatePlanRequest(userId, "Create a beginner workout plan"))

        assertEquals(GroundingStatus.NO_GROUNDING, response.groundingStatus)
        assertEquals(PlanIntent.WORKOUT, response.intent)
        assertEquals(emptyList(), response.plan)
    }

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
        val response = PlanningService(
            retrievalService,
            aiClient,
            PlanningPreparationService(),
            mock(UserCollectionRepository::class.java),
            mock(UserCollectionSourceRepository::class.java),
        ).createPlan(
            CreatePlanRequest(userId = userId, query = "Plan a deep work session"),
        )

        assertEquals(listOf(memory.id), response.retrievedMemoryIds)
        assertEquals(listOf(memory.id), response.plan.single().memoryIds)
    }

    @Test
    fun `does not generate general plan when selected collection has no relevant memory`() {
        val repository = mock(MemoryRepository::class.java)
        val collections = mock(UserCollectionRepository::class.java)
        val sources = mock(UserCollectionSourceRepository::class.java)
        val userId = UUID.randomUUID()
        val collection = UserCollection(
            userId = userId,
            name = "Workout",
            description = null,
        )
        `when`(repository.findAllByUserIdOrderByCreatedAtDesc(userId)).thenReturn(emptyList())
        `when`(collections.findByIdAndUserId(collection.id, userId)).thenReturn(collection)
        `when`(sources.findAllByCollectionIdAndUserIdOrderByCreatedAtAsc(collection.id, userId))
            .thenReturn(emptyList())
        val aiClient = object : AiPlanningClient {
            override fun embedQuery(query: String) = FloatArray(768)
            override fun createPlan(request: AiPlanRequest) =
                error("General generation must not run for a selected collection")
        }

        val response = PlanningService(
            MemoryRetrievalService(repository, aiClient),
            aiClient,
            PlanningPreparationService(),
            collections,
            sources,
        ).createPlan(
            CreatePlanRequest(
                userId = userId,
                query = "Create a study plan",
                constraints = mapOf("collectionIds" to listOf(collection.id.toString())),
                allowGeneralKnowledge = true,
            ),
        )

        assertEquals(GroundingStatus.NO_GROUNDING, response.groundingStatus)
        assertEquals(emptyList(), response.plan)
    }

    @Test
    fun `selected collection excludes relevant memories and citations from other collections`() {
        val repository = mock(MemoryRepository::class.java)
        val collections = mock(UserCollectionRepository::class.java)
        val sources = mock(UserCollectionSourceRepository::class.java)
        val userId = UUID.randomUUID()
        val selectedMemory = memory(
            userId = userId,
            title = "Selected workout routine",
            summary = "Focused strength workout routine.",
            embeddingValue = 0.1f,
        )
        val otherMemory = memory(
            userId = userId,
            title = "Other workout routine",
            summary = "Focused strength workout routine.",
            embeddingValue = 0.1f,
        )
        val collection = UserCollection(
            userId = userId,
            name = "Selected",
            description = null,
        )
        val selectedSource = UserCollectionSource(
            collectionId = collection.id,
            userId = userId,
            url = selectedMemory.sourceUrl,
            userNote = null,
            memoryId = selectedMemory.id,
        )
        `when`(repository.findAllByUserIdOrderByCreatedAtDesc(userId))
            .thenReturn(listOf(otherMemory, selectedMemory))
        `when`(collections.findByIdAndUserId(collection.id, userId)).thenReturn(collection)
        `when`(sources.findAllByCollectionIdAndUserIdOrderByCreatedAtAsc(collection.id, userId))
            .thenReturn(listOf(selectedSource))
        var suppliedMemoryIds = emptyList<String>()
        val aiClient = object : AiPlanningClient {
            override fun embedQuery(query: String) = FloatArray(768) { 0.1f }
            override fun createPlan(request: AiPlanRequest): AiPlanResponse {
                suppliedMemoryIds = request.memories.map(AiPlanningMemory::id)
                return AiPlanResponse(
                    goal = request.query,
                    explanation = "Uses only the selected collection.",
                    plan = listOf(
                        AiPlanStep(
                            step = "Use selected routine",
                            durationMinutes = 30,
                            reason = "Selected source",
                            memoryIds = listOf(selectedMemory.id.toString()),
                        ),
                        AiPlanStep(
                            step = "Use wrong routine",
                            durationMinutes = 30,
                            reason = "Wrong source",
                            memoryIds = listOf(otherMemory.id.toString()),
                        ),
                    ),
                )
            }
        }

        val response = PlanningService(
            MemoryRetrievalService(repository, aiClient),
            aiClient,
            PlanningPreparationService(),
            collections,
            sources,
        ).createPlan(
            CreatePlanRequest(
                userId = userId,
                query = "Create a focused strength workout routine",
                constraints = mapOf("collectionIds" to listOf(collection.id.toString())),
            ),
        )

        assertEquals(listOf(selectedMemory.id.toString()), suppliedMemoryIds)
        assertEquals(listOf(selectedMemory.id), response.retrievedMemoryIds)
        assertEquals(listOf(selectedMemory.id), response.groundingMemories.map { it.id })
        assertEquals(1, response.plan.size)
        assertEquals(listOf(selectedMemory.id), response.plan.single().memoryIds)
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

    @Test
    fun `cafe hopping query rejects gardening memory despite high semantic score`() {
        val repository = mock(MemoryRepository::class.java)
        val userId = UUID.randomUUID()
        val gardening = memory(
            userId = userId,
            title = "Balcony gardening guide",
            summary = "Grow herbs with soil, sunlight, and regular watering.",
            embeddingValue = 0.1f,
        )
        `when`(repository.findAllByUserIdOrderByCreatedAtDesc(userId))
            .thenReturn(listOf(gardening))
        val aiClient = object : AiPlanningClient {
            override fun embedQuery(query: String) = FloatArray(768) { 0.1f }
            override fun createPlan(request: AiPlanRequest) = error("not used")
        }

        val results = MemoryRetrievalService(repository, aiClient).retrieveRelevant(
            userId = userId,
            query = "Plan cafe hopping this weekend",
            intent = PlanIntent.OUTING,
        )

        assertEquals(emptyList(), results)
    }

    @Test
    fun `irrelevant memory returns no grounding instead of a plan`() {
        val repository = mock(MemoryRepository::class.java)
        val userId = UUID.randomUUID()
        val gardening = memory(
            userId = userId,
            title = "Balcony gardening guide",
            summary = "Grow herbs with soil, sunlight, and regular watering.",
            embeddingValue = 0.1f,
        )
        `when`(repository.findAllByUserIdOrderByCreatedAtDesc(userId))
            .thenReturn(listOf(gardening))
        val aiClient = object : AiPlanningClient {
            override fun embedQuery(query: String) = FloatArray(768) { 0.1f }
            override fun createPlan(request: AiPlanRequest) =
                error("Planner must not run with only irrelevant memory")
        }

        val response = PlanningService(
            MemoryRetrievalService(repository, aiClient),
            aiClient,
            PlanningPreparationService(),
            mock(UserCollectionRepository::class.java),
            mock(UserCollectionSourceRepository::class.java),
        ).createPlan(
            CreatePlanRequest(userId = userId, query = "Plan cafe hopping this weekend"),
        )

        assertEquals(GroundingStatus.NO_GROUNDING, response.groundingStatus)
        assertEquals(emptyList(), response.retrievedMemoryIds)
        assertEquals(emptyList(), response.groundingMemories)
        assertEquals(emptyList(), response.plan)
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
