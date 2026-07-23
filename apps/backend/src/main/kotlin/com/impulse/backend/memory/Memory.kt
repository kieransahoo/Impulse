package com.impulse.backend.memory

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import org.hibernate.annotations.Array
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "memories")
class Memory(
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
    val title: String,

    @Column(columnDefinition = "text")
    val description: String?,

    @Column(name = "thumbnail_url", length = 2_000)
    val thumbnailUrl: String? = null,

    @Column(nullable = false, columnDefinition = "text")
    val summary: String,

    @Column(nullable = false, length = 100)
    val category: String,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "memory_tags", joinColumns = [JoinColumn(name = "memory_id")])
    @Column(name = "tag", nullable = false, length = 100)
    val tags: MutableSet<String> = linkedSetOf(),

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "memory_topics", joinColumns = [JoinColumn(name = "memory_id")])
    @Column(name = "topic", nullable = false, length = 150)
    val topics: MutableSet<String> = linkedSetOf(),

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "memory_actions", joinColumns = [JoinColumn(name = "memory_id")])
    val actions: MutableList<MemoryAction> = mutableListOf(),

    @Column(name = "user_note", columnDefinition = "text")
    val userNote: String?,

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 768)
    @Column(nullable = false, columnDefinition = "vector(768)")
    val embedding: FloatArray,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
) {
    @PrePersist
    fun beforeInsert() {
        createdAt = Instant.now()
    }
}

enum class MemoryPlatform {
    YOUTUBE_PLAYLIST,
    YOUTUBE_VIDEO,
    INSTAGRAM,
    WEB,
}

@Embeddable
data class MemoryAction(
    @Column(name = "action", nullable = false, length = 1_000)
    val action: String = "",

    @Column(name = "use_when", columnDefinition = "text")
    val useWhen: String? = null,

    @Column(name = "duration_minutes")
    val durationMinutes: Int? = null,

    @Column(name = "action_category", length = 100)
    val category: String? = null,
)
