package com.impulse.backend.post

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "posts")
class Post(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, length = 2_000)
    var caption: String,

    @Column(name = "image_url", nullable = false, length = 2_000)
    var imageUrl: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var category: PostCategory,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "post_tags", joinColumns = [JoinColumn(name = "post_id")])
    @Column(name = "tag", nullable = false, length = 100)
    var tags: MutableSet<String> = linkedSetOf(),

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    @PrePersist
    fun beforeInsert() {
        val now = Instant.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun beforeUpdate() {
        updatedAt = Instant.now()
    }
}

enum class PostCategory {
    CAFE,
    SHOPPING,
    RECIPE,
    TRAVEL,
    AI,
}

