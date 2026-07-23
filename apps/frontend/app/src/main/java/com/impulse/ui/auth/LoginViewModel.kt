package com.impulse.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.impulse.data.local.SessionManager
import com.impulse.data.model.AuthResponse
import com.impulse.data.model.UserSession
import com.impulse.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginUiState {
    data object Idle : LoginUiState()
    data object Loading : LoginUiState()
    data object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel : ViewModel() {
    private val repository = AuthRepository()
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun signIn(context: Context, email: String, password: String) {
        authenticate(context, email, null, password)
    }

    fun register(context: Context, email: String, password: String) {
        authenticate(context, email, email.substringBefore("@").ifBlank { "User" }, password)
    }

    private fun authenticate(
        context: Context,
        email: String,
        displayName: String?,
        password: String
    ) {
        val normalizedEmail = email.trim().lowercase()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
            _uiState.value = LoginUiState.Error("Enter a valid email address.")
            return
        }
        if (password.length < 8) {
            _uiState.value = LoginUiState.Error("Password must contain at least 8 characters.")
            return
        }
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val result = if (displayName == null) {
                repository.login(normalizedEmail, password)
            } else {
                repository.register(normalizedEmail, displayName, password)
            }
            result.fold(
                onSuccess = {
                    SessionManager.getInstance(context).saveSession(it.toSession())
                    _uiState.value = LoginUiState.Success
                },
                onFailure = {
                    _uiState.value = LoginUiState.Error(
                        it.localizedMessage ?: "Authentication failed. Please try again."
                    )
                }
            )
        }
    }

    fun resetError() {
        _uiState.value = LoginUiState.Idle
    }
}

private fun AuthResponse.toSession() = UserSession(
    userId = userId,
    email = email,
    displayName = displayName,
    photoUrl = null,
    idToken = token
)
