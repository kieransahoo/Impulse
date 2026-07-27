package com.impulse.backend.savedplan

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SavedPlanRepository : JpaRepository<SavedPlan, UUID> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: UUID): List<SavedPlan>
    fun findByIdAndUserId(id: UUID, userId: UUID): SavedPlan?
    fun findFirstByUserIdAndStatusOrderByActivatedAtDesc(
        userId: UUID,
        status: SavedPlanStatus,
    ): SavedPlan?
}

interface SavedPlanStepRepository : JpaRepository<SavedPlanStep, UUID> {
    fun findAllByPlanIdOrderByOrderAsc(planId: UUID): List<SavedPlanStep>
    fun findByIdAndPlanId(id: UUID, planId: UUID): SavedPlanStep?
    fun deleteAllByPlanId(planId: UUID)
}
