package com.impulse.data.repository

import com.impulse.data.api.RetrofitClient
import com.impulse.data.model.LoginRequest
import com.impulse.data.model.RegisterRequest

class AuthRepository {
    private val api = RetrofitClient.apiService

    suspend fun register(email: String, displayName: String, password: String) =
        runCatching { api.register(RegisterRequest(email, displayName, password)) }

    suspend fun login(email: String, password: String) =
        runCatching { api.login(LoginRequest(email, password)) }

    suspend fun logout(token: String) =
        runCatching { api.logout("Bearer $token") }
}
