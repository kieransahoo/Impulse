package com.impulse.backend.usercollection

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserCollectionRepository : JpaRepository<UserCollection, UUID> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: UUID): List<UserCollection>
    fun findByIdAndUserId(id: UUID, userId: UUID): UserCollection?
}

interface UserCollectionSourceRepository : JpaRepository<UserCollectionSource, UUID> {
    fun findAllByCollectionIdAndUserIdOrderByCreatedAtAsc(
        collectionId: UUID,
        userId: UUID,
    ): List<UserCollectionSource>
}
