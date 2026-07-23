package com.impulse.backend.auth

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "app_users")
class AppUser(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true, length = 320)
    val email: String,

    @Column(name = "display_name", nullable = false, length = 100)
    val displayName: String,

    @Column(name = "password_hash", nullable = false, length = 100)
    val passwordHash: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)

@Entity
@Table(name = "auth_sessions")
class AuthSession(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    val tokenHash: String,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)
