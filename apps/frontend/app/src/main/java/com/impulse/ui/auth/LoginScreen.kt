package com.impulse.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.impulse.ui.theme.DividerColor
import com.impulse.ui.theme.ErrorColor
import com.impulse.ui.theme.Hint
import com.impulse.ui.theme.Ink
import com.impulse.ui.theme.Paper
import com.impulse.ui.theme.Primary
import com.impulse.ui.theme.SurfaceBright

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val uiState by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf("") }
    var registering by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) onLoginSuccess()
    }

    val submit = {
        keyboard?.hide()
        if (registering) viewModel.register(context, email, displayName, password)
        else viewModel.signIn(context, email, password)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ImpulseMark()
            Spacer(Modifier.size(10.dp))
            Text("Impulse", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(48.dp))
        Text("PERSONAL MEMORY ENGINE", style = MaterialTheme.typography.labelSmall, color = Primary)
        Spacer(Modifier.height(12.dp))
        Text(
            if (registering) "Create your memory space." else "Welcome back.",
            style = MaterialTheme.typography.displaySmall,
            color = Ink,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            if (registering) "Save useful content and turn it into plans you can act on."
            else "Sign in to use everything you have saved.",
            style = MaterialTheme.typography.bodyMedium,
            color = Hint,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        if (registering) {
            AuthField(
                value = displayName,
                onValueChange = { displayName = it; viewModel.resetError() },
                label = "Name",
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            )
            Spacer(Modifier.height(14.dp))
        }
        AuthField(
            value = email,
            onValueChange = { email = it; viewModel.resetError() },
            label = "Email",
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        )
        Spacer(Modifier.height(14.dp))
        AuthField(
            value = password,
            onValueChange = { password = it; viewModel.resetError() },
            label = "Password",
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            password = true,
            passwordVisible = passwordVisible,
            onTogglePassword = { passwordVisible = !passwordVisible },
            onDone = submit
        )
        Text(
            if (registering) "Use at least 8 characters." else "Your password is never stored on this device.",
            style = MaterialTheme.typography.bodySmall,
            color = Hint,
            modifier = Modifier.fillMaxWidth().padding(top = 7.dp)
        )
        Spacer(Modifier.height(20.dp))

        AnimatedContent(targetState = uiState, label = "auth_state") { state ->
            when (state) {
                LoginUiState.Loading -> CircularProgressIndicator(color = Primary)
                is LoginUiState.Error -> Column {
                    Surface(
                        color = Color(0xFFFFF0EC),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ErrorColor.copy(alpha = .35f))
                    ) {
                        Text(
                            state.message,
                            color = ErrorColor,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth().padding(14.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    AuthButton(registering, submit)
                }
                else -> AuthButton(registering, submit)
            }
        }
        TextButton(
            onClick = {
                registering = !registering
                password = ""
                viewModel.resetError()
            }
        ) {
            Text(
                if (registering) "Already have an account? Sign in"
                else "New to Impulse? Create an account",
                color = Primary
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            "You choose what Impulse remembers. We never collect your browsing history.",
            style = MaterialTheme.typography.bodySmall,
            color = Hint,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    password: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: () -> Unit = {},
    onDone: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        visualTransformation = if (password && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (password) {
            {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            }
        } else null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceBright,
            unfocusedContainerColor = SurfaceBright,
            focusedBorderColor = Primary,
            unfocusedBorderColor = DividerColor
        )
    )
}

@Composable
private fun AuthButton(registering: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Primary)
    ) {
        Text(if (registering) "Create account" else "Sign in")
        Spacer(Modifier.weight(1f))
        Text("→", fontSize = 20.sp)
    }
}

@Composable
private fun ImpulseMark() {
    Box(
        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Ink),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "I",
            color = Color.White,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
    }
}
