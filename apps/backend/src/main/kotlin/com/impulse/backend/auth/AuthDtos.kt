package com.impulse.backend.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class RegisterRequest(
    @field:Email
    @field:NotBlank
    @field:Size(max = 320)
    val email: String,

    @field:NotBlank
    @field:Size(min = 2, max = 100)
    val displayName: String,

    @field:NotBlank
    @field:Size(min = 8, max = 72)
    val password: String,
)

data class LoginRequest(
    @field:Email
    @field:NotBlank
    @field:Size(max = 320)
    val email: String,

    @field:NotBlank
    @field:Size(max = 72)
    val password: String,
)

data class AuthResponse(
    val userId: UUID,
    val email: String,
    val displayName: String,
    val token: String,
    val expiresAt: Instant,
)
