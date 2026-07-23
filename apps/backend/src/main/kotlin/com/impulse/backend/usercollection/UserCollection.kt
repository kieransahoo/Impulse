package com.impulse.backend.usercollection

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "user_collections")
class UserCollection(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(nullable = false, length = 200)
    val name: String,

    @Column(columnDefinition = "text")
    val description: String?,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
) {
    @PrePersist
    fun beforeInsert() {
        createdAt = Instant.now()
    }
}

@Entity
@Table(name = "user_collection_sources")
class UserCollectionSource(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "collection_id", nullable = false)
    val collectionId: UUID,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(nullable = false, length = 2_000)
    val url: String,

    @Column(name = "user_note", columnDefinition = "text")
    val userNote: String?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: CollectionSourceStatus = CollectionSourceStatus.PENDING,

    @Column(name = "memory_id")
    var memoryId: UUID? = null,

    @Column(name = "error_message", columnDefinition = "text")
    var errorMessage: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "processed_at")
    var processedAt: Instant? = null,
) {
    @PrePersist
    fun beforeInsert() {
        createdAt = Instant.now()
    }
}

enum class CollectionSourceStatus {
    PENDING,
    PROCESSED,
    FAILED,
}
