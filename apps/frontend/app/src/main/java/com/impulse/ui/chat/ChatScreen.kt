package com.impulse.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.impulse.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val messages  by viewModel.messages.collectAsState()
    val isTyping  by viewModel.isTyping.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState  = rememberLazyListState()
    val scope      = rememberCoroutineScope()

    LaunchedEffect(userId) {
        viewModel.setUserId(userId)
    }

    // Auto-scroll to bottom on new message
    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty() || isTyping) {
            scope.launch {
                listState.animateScrollToItem(
                    index = messages.size + (if (isTyping) 0 else -1)
                )
            }
        }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            ChatTopBar(
                onBack      = onNavigateBack,
                onClearChat = { viewModel.clearConversation() }
            )
        },
        bottomBar = {
            ChatInputBar(
                text         = inputText,
                onTextChange = { inputText = it },
                isLoading    = isTyping,
                onSend       = {
                    val trimmed = inputText.trim()
                    if (trimmed.isNotEmpty()) {
                        viewModel.sendMessage(trimmed)
                        inputText = ""
                    }
                }
            )
        }
    ) { padding ->
        if (messages.isEmpty() && !isTyping) {
            ChatWelcomeState(
                modifier  = Modifier.padding(padding),
                onSuggest = { suggestion ->
                    viewModel.sendMessage(suggestion)
                }
            )
        } else {
            LazyColumn(
                state          = listState,
                modifier       = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    AnimatedVisibility(
                        visible = true,
                        enter   = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })
                    ) {
                        MessageBubble(message = msg)
                    }
                }

                // Typing indicator
                if (isTyping) {
                    item(key = "typing") {
                        TypingIndicator()
                    }
                }
            }
        }
    }
}

// ─── Top bar ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(onBack: () -> Unit, onClearChat: () -> Unit) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OnBackground)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier        = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Primary, Secondary))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Use your memories", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OnBackground)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(SuccessColor)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Ready to retrieve", fontSize = 12.sp, color = SuccessColor)
                    }
                }
            }
        },
        actions = {
            IconButton(onClick = onClearChat) {
                Icon(Icons.Default.DeleteOutline, "Clear chat", tint = Hint)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
    )
}

// ─── Message bubble ───────────────────────────────────────────────────────────

@Composable
private fun MessageBubble(message: UiMessage) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment     = Alignment.Bottom
    ) {
        if (!message.isUser) {
            AiAvatar()
            Spacer(Modifier.width(6.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .clip(
                    if (message.isUser)
                        RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp)
                    else
                        RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)
                )
                .then(
                    if (message.isUser)
                        Modifier.background(Brush.linearGradient(listOf(Primary, PrimaryVariant)))
                    else
                        Modifier
                            .background(AiBubble)
                )
                .then(
                    if (!message.isUser)
                        Modifier.then(
                            Modifier  // border via Compose border modifier
                        )
                    else Modifier
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text       = message.content,
                color      = if (message.isUser) UserText else AiText,
                fontSize   = 15.sp,
                lineHeight = 22.sp
            )
            if (!message.isUser && message.sources.isNotEmpty()) {
                Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    message.sources.take(4).forEach { source ->
                        Surface(
                            color = NeutralContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "${source.platform.replace("_", " ")} · ${source.title}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Ink,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }

        if (message.isUser) {
            Spacer(Modifier.width(6.dp))
            UserAvatar()
        }
    }
}

@Composable
private fun AiAvatar() {
    Box(
        modifier        = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Primary, Secondary))),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun UserAvatar() {
    Box(
        modifier        = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(SurfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Person, null, tint = OnSurface, modifier = Modifier.size(18.dp))
    }
}

// ─── Typing indicator ─────────────────────────────────────────────────────────

@Composable
private fun TypingIndicator() {
    Row(
        modifier          = Modifier
            .padding(horizontal = 14.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        AiAvatar()
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp))
                .background(AiBubble)
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            AnimatedDots()
        }
    }
}

@Composable
private fun AnimatedDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_dots")
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        repeat(3) { i ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.25f,
                targetValue  = 1f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1200
                        0.25f at 0 using LinearEasing
                        1f    at (300 * (i + 1)) using LinearEasing
                        0.25f at 900 using LinearEasing
                        0.25f at 1200
                    },
                    repeatMode = RepeatMode.Restart
                ),
                label = "dot_alpha_$i"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = alpha))
            )
        }
    }
}

// ─── Input bar ────────────────────────────────────────────────────────────────

@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    isLoading: Boolean,
    onSend: () -> Unit
) {
    Surface(
        color           = Surface,
        shadowElevation = 8.dp,
        modifier        = Modifier.navigationBarsPadding()
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value           = text,
                onValueChange   = onTextChange,
                placeholder     = {
                    Text("What do you want to plan or decide?", color = Hint, fontSize = 14.sp)
                },
                modifier        = Modifier.weight(1f),
                shape           = RoundedCornerShape(24.dp),
                colors          = OutlinedTextFieldDefaults.colors(
                    focusedTextColor      = OnBackground,
                    unfocusedTextColor    = OnBackground,
                    focusedContainerColor   = InputBackground,
                    unfocusedContainerColor = InputBackground,
                    focusedBorderColor    = Primary.copy(alpha = 0.55f),
                    unfocusedBorderColor  = InputBorder,
                    cursorColor           = Primary
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                maxLines        = 5
            )

            Spacer(Modifier.width(8.dp))

            val canSend = !isLoading && text.isNotBlank()

            Box(
                modifier        = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend)
                            Brush.linearGradient(listOf(Primary, Secondary))
                        else
                            Brush.linearGradient(listOf(SurfaceVariant, SurfaceVariant))
                    )
                    .clickable(enabled = canSend) { onSend() },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color       = Hint,
                        strokeWidth = 2.dp,
                        modifier    = Modifier.size(22.dp)
                    )
                } else {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint               = if (canSend) Color.White else Hint,
                        modifier           = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// ─── Welcome / empty state ────────────────────────────────────────────────────

@Composable
private fun ChatWelcomeState(
    modifier: Modifier = Modifier,
    onSuggest: (String) -> Unit
) {
    val suggestions = listOf(
        "Plan a trip from my saved travel ideas",
        "Compare products I have saved",
        "Create a meal plan from my recipes",
        "Build a focused learning plan"
    )

    Column(
        modifier              = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.Center
    ) {
        Box(
            modifier        = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Primary, Secondary))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint               = Color.White,
                modifier           = Modifier.size(44.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text       = "What do you want to do?",
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            color      = OnBackground,
            textAlign  = TextAlign.Center
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text      = "Impulse will use your saved knowledge when it is relevant.",
            fontSize  = 14.sp,
            color     = Hint,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            suggestions.forEach { suggestion ->
                OutlinedButton(
                    onClick  = { onSuggest(suggestion) },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(14.dp),
                    border   = BorderStroke(1.dp, DividerColor),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        containerColor = SurfaceVariant.copy(alpha = 0.5f),
                        contentColor   = OnSurface
                    )
                ) {
                    Text(
                        text     = suggestion,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
