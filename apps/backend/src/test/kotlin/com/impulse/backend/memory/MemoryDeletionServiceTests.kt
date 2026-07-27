package com.impulse.backend.memory

import com.impulse.backend.usercollection.UserCollectionSourceRepository
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID
import kotlin.test.Test

class MemoryDeletionServiceTests {
    private val memories = mock(MemoryRepository::class.java)
    private val sources = mock(UserCollectionSourceRepository::class.java)
    private val service = MemoryDeletionService(memories, sources)

    @Test
    fun `deletes owned memory and its collection associations`() {
        val userId = UUID.randomUUID()
        val memory = memory(userId)
        `when`(memories.findByIdAndUserId(memory.id, userId)).thenReturn(memory)

        service.delete(memory.id, userId)

        verify(sources).deleteAllByUserIdAndMemoryId(userId, memory.id)
        verify(memories).delete(memory)
    }

    @Test
    fun `does not delete a memory owned by another user`() {
        val userId = UUID.randomUUID()
        val memoryId = UUID.randomUUID()
        `when`(memories.findByIdAndUserId(memoryId, userId)).thenReturn(null)

        assertThrows<MemoryNotFoundException> { service.delete(memoryId, userId) }

        verify(sources, never()).deleteAllByUserIdAndMemoryId(userId, memoryId)
    }

    @Test
    fun `clears user sources before user memories`() {
        val userId = UUID.randomUUID()

        service.clear(userId)

        verify(sources).deleteAllByUserId(userId)
        verify(memories).deleteAllByUserId(userId)
    }

    private fun memory(userId: UUID) = Memory(
        userId = userId,
        sourceUrl = "https://example.com/memory",
        platform = MemoryPlatform.WEB,
        title = "Memory",
        description = null,
        summary = "Summary",
        category = "Ideas",
        userNote = null,
        embedding = FloatArray(768),
    )
}
