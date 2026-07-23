package com.impulse.backend.collection

import com.impulse.backend.memory.MemoryPlatform
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "link_collections")
class LinkCollection(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "source_url", nullable = false, length = 2_000)
    val sourceUrl: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val platform: MemoryPlatform,

    @Column(nullable = false, length = 500)
    var title: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: LinkCollectionStatus = LinkCollectionStatus.COLLECTED,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "collection_id", nullable = false)
    @OrderBy("position ASC")
    val items: MutableList<CollectedLink> = mutableListOf(),

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
) {
    @PrePersist
    fun beforeInsert() {
        createdAt = Instant.now()
    }
}

@Entity
@Table(name = "collected_links")
class CollectedLink(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, length = 2_000)
    val url: String,

    @Column(nullable = false, length = 500)
    val title: String,

    @Column(columnDefinition = "text")
    val description: String?,

    @Column(name = "thumbnail_url", length = 2_000)
    val thumbnailUrl: String?,

    @Column(nullable = false)
    val position: Int,
)

enum class LinkCollectionStatus {
    COLLECTED,
    PROCESSED,
}
