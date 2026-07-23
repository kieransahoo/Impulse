package com.impulse.ui.auth

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.impulse.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) onLoginSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFF0D0D1A),
                        0.5f to Color(0xFF1A0A2E),
                        1.0f to Color(0xFF0D0D1A)
                    )
                )
            )
    ) {
        // Decorative glow blobs
        GlowBlob(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset((-60).dp, (-60).dp),
            color = Primary.copy(alpha = 0.12f),
            size = 280.dp
        )
        GlowBlob(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(60.dp, 60.dp),
            color = Secondary.copy(alpha = 0.10f),
            size = 220.dp
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── App logo ──────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(listOf(Primary, Secondary))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Impulse logo",
                    tint = Color.White,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Impulse",
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                color = OnBackground
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Share links. Chat with AI.",
                fontSize = 16.sp,
                color = Hint,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            // ── Feature chips ────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FeatureChip("🔗 Share URLs")
                FeatureChip("🤖 AI Chat")
                FeatureChip("⚡ Instant")
            }

            Spacer(Modifier.height(56.dp))

            // ── Sign-in area ─────────────────────────────────────────────────
            AnimatedContent(
                targetState = uiState,
                label = "login_state"
            ) { state ->
                when (state) {
                    is LoginUiState.Loading -> {
                        CircularProgressIndicator(
                            color = Primary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(52.dp)
                        )
                    }

                    is LoginUiState.Error -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ErrorColor.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, ErrorColor.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = state.message,
                                    color = ErrorColor,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 10.dp
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            GoogleSignInButton {
                                viewModel.setLoading()
                                coroutineScope.launch {
                                    viewModel.signInWithGoogle(context)
                                }
                            }
                        }
                    }

                    else -> {
                        GoogleSignInButton {
                            viewModel.setLoading()
                            coroutineScope.launch {
                                viewModel.signInWithGoogle(context)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "By continuing, you agree to our Terms of Service\nand Privacy Policy.",
                fontSize = 12.sp,
                color = Hint,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun GlowBlob(
    modifier: Modifier,
    color: Color,
    size: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(listOf(color, Color.Transparent))
            )
    )
}

@Composable
private fun FeatureChip(label: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SurfaceVariant.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, DividerColor)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            color = OnSurface
        )
    }
}

@Composable
private fun GoogleSignInButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF1E1E38),
            contentColor = OnBackground
        ),
        border = BorderStroke(1.dp, DividerColor),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Coloured "G" as a stand-in for the Google logo (no asset dependency)
            Text("G", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4285F4))
            Text("o", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA4335))
            Text("o", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBC05))
            Text("g", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34A853))
            Text("l", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA4335))
            Text("e", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4285F4))

            Spacer(Modifier.width(14.dp))

            Text(
                text = "Continue with Google",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnBackground
            )
        }
    }
}
