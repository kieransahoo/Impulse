package com.impulse.backend.usercollection

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserCollectionRepository : JpaRepository<UserCollection, UUID> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: UUID): List<UserCollection>
    fun findByIdAndUserId(id: UUID, userId: UUID): UserCollection?
    fun findByUserIdAndNameIgnoreCase(userId: UUID, name: String): UserCollection?
}

interface UserCollectionSourceRepository : JpaRepository<UserCollectionSource, UUID> {
    fun existsByCollectionIdAndUserIdAndUrl(collectionId: UUID, userId: UUID, url: String): Boolean
    fun findByIdAndCollectionIdAndUserId(id: UUID, collectionId: UUID, userId: UUID): UserCollectionSource?
    fun deleteAllByCollectionIdAndUserId(collectionId: UUID, userId: UUID)
    fun deleteAllByUserIdAndMemoryId(userId: UUID, memoryId: UUID)
    fun deleteAllByUserId(userId: UUID)
    fun findAllByCollectionIdAndUserIdOrderByCreatedAtAsc(
        collectionId: UUID,
        userId: UUID,
    ): List<UserCollectionSource>
}
