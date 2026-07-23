package com.impulse.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.impulse.data.local.SessionManager
import com.impulse.ui.auth.LoginScreen
import com.impulse.ui.chat.ChatScreen
import com.impulse.ui.home.HomeScreen
import com.impulse.data.repository.AuthRepository
import kotlinx.coroutines.launch

// ─── Route definitions ────────────────────────────────────────────────────────

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home  : Screen("home")
    object Chat  : Screen("chat")
}

// ─── Root nav graph ───────────────────────────────────────────────────────────

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    val context        = LocalContext.current
    val sessionManager = remember { SessionManager.getInstance(context) }
    val authRepository = remember { AuthRepository() }
    val scope = rememberCoroutineScope()

    val start = if (sessionManager.isLoggedIn()) Screen.Home.route else Screen.Login.route

    NavHost(navController = navController, startDestination = start) {

        // ── Login ─────────────────────────────────────────────────────────────
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Home ──────────────────────────────────────────────────────────────
        composable(Screen.Home.route) {
            val session = remember { sessionManager.getSession() }
            HomeScreen(
                session          = session,
                onNavigateToChat = { navController.navigate(Screen.Chat.route) },
                onSignOut        = {
                    val token = sessionManager.getIdToken()
                    scope.launch {
                        authRepository.logout(token)
                        sessionManager.clearSession()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        // ── Chat ──────────────────────────────────────────────────────────────
        composable(Screen.Chat.route) {
            val session = remember { sessionManager.getSession() }
            ChatScreen(
                userId = session?.userId.orEmpty(),
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
