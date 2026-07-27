package com.impulse.backend.memory

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MemoryRepository : JpaRepository<Memory, UUID> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: UUID): List<Memory>

    fun existsByUserIdAndSourceUrl(userId: UUID, sourceUrl: String): Boolean

    fun findByUserIdAndSourceUrl(userId: UUID, sourceUrl: String): Memory?
    fun findByIdAndUserId(id: UUID, userId: UUID): Memory?
    fun deleteAllByUserId(userId: UUID)
}
