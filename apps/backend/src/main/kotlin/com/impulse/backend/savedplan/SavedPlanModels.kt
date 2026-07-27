package com.impulse.backend.savedplan

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

enum class SavedPlanStatus {
    SAVED,
    ACTIVE,
    COMPLETED,
}

@Entity
@Table(name = "saved_plans")
class SavedPlan(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(nullable = false, length = 500)
    var goal: String,

    @Column(nullable = false, columnDefinition = "text")
    var explanation: String,

    @Column(name = "retrieved_memory_ids", columnDefinition = "text")
    var retrievedMemoryIds: String?,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_status")
    var status: SavedPlanStatus? = SavedPlanStatus.SAVED,

    @Column(name = "activated_at")
    var activatedAt: Instant? = null,

    @Column(name = "completed_at")
    var completedAt: Instant? = null,
)

@Entity
@Table(name = "saved_plan_steps")
class SavedPlanStep(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "plan_id", nullable = false)
    val planId: UUID,

    @Column(name = "step_order", nullable = false)
    val order: Int,

    @Column(nullable = false, length = 2_000)
    val step: String,

    @Column(name = "duration_minutes")
    val durationMinutes: Int?,

    @Column(columnDefinition = "text")
    val reason: String?,

    @Column(name = "memory_ids", columnDefinition = "text")
    val memoryIds: String?,

    @Column(name = "is_completed")
    var completed: Boolean? = false,

    @Column(name = "completed_at")
    var completedAt: Instant? = null,
)
