package com.impulse.data.local

import android.content.Context
import android.content.SharedPreferences
import com.impulse.data.model.UserSession

/**
 * Persists user session data in SharedPreferences.
 * Thread-safe singleton via double-checked locking.
 */
class SessionManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "impulse_session"

        private const val KEY_USER_ID      = "user_id"
        private const val KEY_EMAIL        = "email"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_PHOTO_URL    = "photo_url"
        private const val KEY_ID_TOKEN     = "id_token"
        private const val KEY_LOGGED_IN    = "is_logged_in"

        @Volatile private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
    }

    fun saveSession(session: UserSession) {
        prefs.edit()
            .putString(KEY_USER_ID,      session.userId)
            .putString(KEY_EMAIL,        session.email)
            .putString(KEY_DISPLAY_NAME, session.displayName)
            .putString(KEY_PHOTO_URL,    session.photoUrl)
            .putString(KEY_ID_TOKEN,     session.idToken)
            .putBoolean(KEY_LOGGED_IN,   true)
            .apply()
    }

    fun getSession(): UserSession? {
        if (!isLoggedIn()) return null
        return UserSession(
            userId      = prefs.getString(KEY_USER_ID,      "") ?: "",
            email       = prefs.getString(KEY_EMAIL,        "") ?: "",
            displayName = prefs.getString(KEY_DISPLAY_NAME, "User") ?: "User",
            photoUrl    = prefs.getString(KEY_PHOTO_URL,    null),
            idToken     = prefs.getString(KEY_ID_TOKEN,     "") ?: ""
        )
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_LOGGED_IN, false)

    fun getIdToken(): String = prefs.getString(KEY_ID_TOKEN, "") ?: ""

    fun clearSession() = prefs.edit().clear().apply()
}
