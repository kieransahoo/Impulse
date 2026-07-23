package com.impulse.backend.memory

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class MemoryServiceTests {
    @Test
    fun `stores AI result as a user memory with a 768 dimension vector`() {
        val repository = mock(MemoryRepository::class.java)
        val processor = object : AiMemoryProcessor {
            override fun process(request: AiMemoryRequest) = AiMemoryResult(
                title = "Useful planning idea",
                description = "A selected YouTube video",
                summary = "An idea saved for a future plan.",
                category = "Planning",
                tags = setOf("ideas", "planning"),
                topics = setOf("deep work"),
                actions = listOf(
                    AiMemoryAction(
                        action = "Schedule a focus block",
                        useWhen = listOf("coding"),
                        durationMinutes = 90,
                        category = "productivity",
                    ),
                ),
                embedding = List(768) { 0.01f },
            )
        }
        val service = MemoryService(repository, MemorySourceParser(), processor)
        val userId = UUID.randomUUID()
        val url = "https://www.youtube.com/watch?v=abc"

        `when`(repository.existsByUserIdAndSourceUrl(userId, url)).thenReturn(false)
        `when`(repository.save(any(Memory::class.java))).thenAnswer { it.arguments.first() }

        val response = service.import(
            ImportMemoryRequest(
                userId = userId,
                url = url,
                userNote = "Use this later",
                content = "Visible selected-page content",
            ),
        )

        assertEquals(userId, response.userId)
        assertEquals("Useful planning idea", response.title)
        val captor = ArgumentCaptor.forClass(Memory::class.java)
        verify(repository).save(captor.capture())
        assertEquals(768, captor.value.embedding.size)
        assertEquals(setOf("deep work"), captor.value.topics)
        assertEquals("Schedule a focus block", captor.value.actions.single().action)
    }
}
