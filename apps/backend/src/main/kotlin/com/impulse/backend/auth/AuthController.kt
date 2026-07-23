package com.impulse.backend.auth

import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(private val service: AuthService) {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterRequest): AuthResponse =
        service.register(request)

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): AuthResponse =
        service.login(request)

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(@RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?) {
        service.logout(authorization?.removePrefix("Bearer ")?.trim())
    }
}
