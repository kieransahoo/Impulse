package com.impulse.ui.auth

import android.content.Context
import androidx.activity.result.ActivityResult
import androidx.core.content.ContextCompat.getString
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.impulse.R
import com.impulse.data.local.SessionManager
import com.impulse.data.model.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ─── UI State ─────────────────────────────────────────────────────────────────

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /** Build the Google sign-in client with the placeholderWeb Client ID. */
    suspend fun signInWithGoogle(context: Context): Boolean {
        val credentialManager = CredentialManager.create(context)
        // Instantiate a Google sign-in request
        val googleIdOption = GetGoogleIdOption.Builder()
            // Your server's client ID, not your Android client ID.
            .setServerClientId(getString(context, R.string.default_web_client_id))
            // Only show accounts previously used to sign in.
            .setFilterByAuthorizedAccounts(true)
            .build()

        // Create the Credential Manager request
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context = context, request = request)
            if (result.credential is GoogleIdTokenCredential) {
                val firebaseCred = GoogleAuthProvider.getCredential(
                    (result.credential as GoogleIdTokenCredential).idToken, null
                )
                FirebaseAuth.getInstance().signInWithCredential(firebaseCred).await()
                /*val session = UserSession(
                    userId = account.id ?: "",
                    email = account.email ?: "",
                    displayName = account.displayName ?: "User",
                    photoUrl = account.photoUrl?.toString(),
                    idToken = account.idToken ?: ""
                )
                SessionManager.getInstance(context).saveSession(session)*/
                _uiState.value = LoginUiState.Success
                true
            } else {
                _uiState.value = LoginUiState.Error("Sign-in failed")
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.value = LoginUiState.Error("Sign-in failed")
            false
        }
    }

    fun setLoading() {
        _uiState.value = LoginUiState.Loading
    }

    fun resetError() {
        _uiState.value = LoginUiState.Idle
    }
}
