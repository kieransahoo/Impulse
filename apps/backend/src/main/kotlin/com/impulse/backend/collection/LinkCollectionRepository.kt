package com.impulse.backend.collection

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface LinkCollectionRepository : JpaRepository<LinkCollection, UUID> {
    fun existsByUserIdAndSourceUrl(userId: UUID, sourceUrl: String): Boolean

    fun findByIdAndUserId(id: UUID, userId: UUID): LinkCollection?
}
