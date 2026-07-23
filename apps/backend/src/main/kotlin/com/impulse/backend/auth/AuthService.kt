package com.impulse.backend.auth

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.Locale
import java.security.SecureRandom

class EmailAlreadyRegisteredException : RuntimeException("An account already exists for this email.")
class InvalidCredentialsException : RuntimeException("The email or password is incorrect.")

@Service
class AuthService(
    private val users: AppUserRepository,
    private val sessions: AuthSessionRepository,
) {
    private val passwordEncoder = BCryptPasswordEncoder(12)
    private val secureRandom = SecureRandom()

    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        val email = request.email.trim().lowercase(Locale.ROOT)
        if (users.existsByEmail(email)) throw EmailAlreadyRegisteredException()
        val user = users.save(
            AppUser(
                email = email,
                displayName = request.displayName.trim(),
                passwordHash = requireNotNull(passwordEncoder.encode(request.password)),
            ),
        )
        return createSession(user)
    }

    @Transactional
    fun login(request: LoginRequest): AuthResponse {
        val email = request.email.trim().lowercase(Locale.ROOT)
        val user = users.findByEmail(email)
            ?.takeIf { passwordEncoder.matches(request.password, it.passwordHash) }
            ?: throw InvalidCredentialsException()
        return createSession(user)
    }

    @Transactional
    fun logout(rawToken: String?) {
        rawToken?.takeIf(String::isNotBlank)?.let {
            sessions.deleteByTokenHash(hashToken(it))
        }
    }

    private fun createSession(user: AppUser): AuthResponse {
        val bytes = ByteArray(32).also(secureRandom::nextBytes)
        val rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        val expiresAt = Instant.now().plus(Duration.ofDays(30))
        sessions.save(
            AuthSession(
                userId = user.id,
                tokenHash = hashToken(rawToken),
                expiresAt = expiresAt,
            ),
        )
        return AuthResponse(user.id, user.email, user.displayName, rawToken, expiresAt)
    }

    private fun hashToken(token: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
