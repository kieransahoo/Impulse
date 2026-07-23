package com.impulse.backend.auth

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AppUserRepository : JpaRepository<AppUser, UUID> {
    fun findByEmail(email: String): AppUser?
    fun existsByEmail(email: String): Boolean
}

interface AuthSessionRepository : JpaRepository<AuthSession, UUID> {
    fun findByTokenHash(tokenHash: String): AuthSession?
    fun deleteByTokenHash(tokenHash: String)
}
