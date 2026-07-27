package com.impulse.backend.memory

import com.impulse.backend.usercollection.UserCollectionSourceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class MemoryDeletionService(
    private val memoryRepository: MemoryRepository,
    private val sourceRepository: UserCollectionSourceRepository,
) {
    @Transactional
    fun delete(memoryId: UUID, userId: UUID) {
        val memory = memoryRepository.findByIdAndUserId(memoryId, userId)
            ?: throw MemoryNotFoundException()
        sourceRepository.deleteAllByUserIdAndMemoryId(userId, memory.id)
        memoryRepository.delete(memory)
    }

    @Transactional
    fun clear(userId: UUID) {
        sourceRepository.deleteAllByUserId(userId)
        memoryRepository.deleteAllByUserId(userId)
    }
}

class MemoryNotFoundException :
    NoSuchElementException("Memory was not found for this user")
