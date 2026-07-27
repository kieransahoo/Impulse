package com.impulse.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.impulse.data.model.UserSession
import com.impulse.data.model.UserCollectionResponse
import coil.compose.AsyncImage
import com.impulse.ui.theme.AccentContainer
import com.impulse.ui.theme.DividerColor
import com.impulse.ui.theme.Hint
import com.impulse.ui.theme.Ink
import com.impulse.ui.theme.NeutralContainer
import com.impulse.ui.theme.Paper
import com.impulse.ui.theme.Primary
import com.impulse.ui.theme.Secondary
import com.impulse.ui.theme.SuccessColor
import com.impulse.ui.theme.Surface
import com.impulse.ui.theme.SurfaceBright
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    session: UserSession?,
    onNavigateToChat: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCapture by remember { mutableStateOf(false) }

    LaunchedEffect(session?.userId) {
        session?.let { viewModel.loadRecentUrls(it.userId) }
    }
    LaunchedEffect(uiState.message, uiState.error) {
        if (uiState.message != null || uiState.error != null) {
            delay(2_000)
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        containerColor = Paper,
        bottomBar = { ImpulseBottomBar(onUseMemories = onNavigateToChat) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item { BrandHeader(session, onSignOut) }
            item { Hero(onNavigateToChat, onAdd = { showCapture = true }) }
            item { QuickActions(onNavigateToChat) }
            item { MemorySummary(uiState.memories.size) }
            if (uiState.loading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
            }
            uiState.error?.let { error ->
                item { FeedbackCard(error, error = true) }
            }
            uiState.message?.let { message ->
                item { FeedbackCard(message, error = false) }
            }
            if (uiState.collections.isNotEmpty()) {
                item {
                    CollectionGalleryHeader(
                        count = uiState.collections.size,
                        onNew = { showCapture = true }
                    )
                }
                items(
                    uiState.collections.chunked(2),
                    key = { row -> row.joinToString("-") { it.id } }
                ) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { collection ->
                            CollectionGalleryCard(
                                collection = collection,
                                memories = uiState.memories,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            item {
                SectionHeader(
                    eyebrow = "YOUR MEMORY",
                    title = "Recently saved",
                    action = "Browse all"
                )
            }
            if (uiState.memories.isEmpty() && !uiState.loading) {
                item { EmptyMemoryState() }
            } else {
                items(uiState.memories, key = { it.id }) { MemoryCard(it) }
            }
        }
    }

    if (showCapture) {
        CaptureDialog(
            creating = uiState.creating,
            onDismiss = { if (!uiState.creating) showCapture = false },
            onCreate = { name, description, urls ->
                val userId = session?.userId ?: return@CaptureDialog
                viewModel.createCollection(userId, name, description, urls)
                showCapture = false
            }
        )
    }
}

@Composable
private fun BrandHeader(session: UserSession?, onSignOut: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ImpulseMark()
        Spacer(Modifier.size(10.dp))
        Text("Impulse", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text(
            text = session?.displayName?.split(" ")?.firstOrNull() ?: "Workspace",
            style = MaterialTheme.typography.bodySmall,
            color = Hint
        )
        IconButton(onClick = onSignOut) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out", tint = Hint)
        }
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
            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
    }
}

@Composable
private fun Hero(onUseMemories: () -> Unit, onAdd: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp)) {
        EditorialLabel("PERSONAL MEMORY ENGINE")
        Spacer(Modifier.height(10.dp))
        Text("Turn saved links into", style = MaterialTheme.typography.displaySmall, color = Ink)
        Text(
            "useful personal context.",
            style = MaterialTheme.typography.displaySmall,
            color = Primary,
            fontStyle = FontStyle.Italic
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "Ask Impulse to plan, compare, or recommend using what you have saved.",
            style = MaterialTheme.typography.bodyMedium,
            color = Hint
        )
        Spacer(Modifier.height(22.dp))
        Button(
            onClick = onUseMemories,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(Modifier.size(10.dp))
            Text("Use your memories", modifier = Modifier.weight(1f))
            Text("→", fontSize = 20.sp)
        }
        TextButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Add links to your memory", color = Primary)
        }
    }
}

@Composable
private fun QuickActions(onSelect: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickAction("Plan a trip", "Travel", Modifier.weight(1f), onSelect)
        QuickAction("Compare products", "Shopping", Modifier.weight(1f), onSelect)
    }
}

@Composable
private fun QuickAction(title: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceBright,
        border = BorderStroke(1.dp, DividerColor)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Primary)
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            Text("Start →", style = MaterialTheme.typography.bodySmall, color = Hint)
        }
    }
}

@Composable
private fun MemorySummary(count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, DividerColor.copy(alpha = .65f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("YOUR MEMORY SPACE", style = MaterialTheme.typography.labelSmall, color = Primary)
                Spacer(Modifier.height(8.dp))
                Text("$count memories ready", style = MaterialTheme.typography.titleLarge, color = Ink)
                Text("Saved knowledge is ready to use.", style = MaterialTheme.typography.bodySmall, color = Hint)
            }
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape).background(AccentContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Link, contentDescription = null, tint = Ink)
            }
        }
    }
}

@Composable
private fun SectionHeader(eyebrow: String, title: String, action: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(Modifier.weight(1f)) {
            EditorialLabel(eyebrow)
            Text(title, style = MaterialTheme.typography.titleLarge)
        }
        Text(action, style = MaterialTheme.typography.labelLarge, color = Primary)
    }
}

@Composable
private fun EditorialLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = Primary)
}

@Composable
private fun MemoryCard(item: SharedUrlItem) {
    val formattedDate = SimpleDateFormat("MMM d", Locale.US).format(Date(item.timestamp))
    val platform = item.platform.replace("_", " ").uppercase()

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, DividerColor)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(58.dp).clip(RoundedCornerShape(12.dp)).background(AccentContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(item.title.take(1).uppercase(), color = Primary, fontSize = 24.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif)
            }
            Spacer(Modifier.size(13.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(platform, style = MaterialTheme.typography.labelSmall, color = SuccessColor)
                    Text("  •  $formattedDate", style = MaterialTheme.typography.bodySmall, color = Hint)
                }
                Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.url, style = MaterialTheme.typography.bodySmall, color = Hint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Open memory", tint = Hint)
        }
    }
}

@Composable
private fun CollectionGalleryHeader(count: Int, onNew: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(Modifier.weight(1f)) {
            EditorialLabel("COLLECTIONS")
            Text("Saved collections", style = MaterialTheme.typography.titleLarge)
            Text("Only you can see what you have saved", style = MaterialTheme.typography.bodySmall, color = Hint)
        }
        TextButton(onClick = onNew) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Primary)
            Text("New", color = Primary)
        }
    }
}

@Composable
private fun CollectionGalleryCard(
    collection: UserCollectionResponse,
    memories: List<SharedUrlItem>,
    modifier: Modifier = Modifier
) {
    val memoryById = memories.associateBy { it.id }
    val covers = collection.sources.mapNotNull { source ->
        source.memoryId?.let(memoryById::get)?.thumbnailUrl
    }.distinct().take(4)

    Card(
        modifier = modifier.padding(vertical = 6.dp).aspectRatio(.82f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, DividerColor.copy(alpha = .65f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            if (covers.isEmpty()) {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.linearGradient(listOf(Primary.copy(alpha = .85f), Ink))
                    )
                )
                Text(
                    collection.name.take(1).uppercase(),
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White.copy(alpha = .35f),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                    fontSize = 72.sp,
                    fontStyle = FontStyle.Italic
                )
            } else {
                CollectionCoverCollage(covers)
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = .82f))
                    )
                )
            )
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(14.dp)
            ) {
                Text(
                    collection.name,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${collection.processedSources} saved",
                    color = Color.White.copy(alpha = .72f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (collection.failedSources > 0) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(9.dp),
                    color = com.impulse.ui.theme.ErrorContainer,
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(
                        "${collection.failedSources} issue",
                        color = com.impulse.ui.theme.ErrorColor,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectionCoverCollage(urls: List<String>) {
    if (urls.size == 1) {
        AsyncImage(
            model = urls.first(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        return
    }
    Column(Modifier.fillMaxSize()) {
        urls.chunked(2).take(2).forEach { row ->
            Row(Modifier.weight(1f)) {
                row.forEach { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxSize()
                    )
                }
                if (row.size == 1) {
                    Box(Modifier.weight(1f).fillMaxSize().background(NeutralContainer))
                }
            }
        }
    }
}

@Composable
private fun FeedbackCard(message: String, error: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        color = if (error) com.impulse.ui.theme.ErrorContainer else com.impulse.ui.theme.SuccessContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = if (error) com.impulse.ui.theme.ErrorColor else SuccessColor,
            modifier = Modifier.padding(14.dp)
        )
    }
}

@Composable
private fun CaptureDialog(
    creating: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, String?, List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var urlsText by remember { mutableStateOf("") }
    val urls = urlsText.lines().map(String::trim).filter { it.startsWith("http://") || it.startsWith("https://") }.distinct().take(20)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add knowledge", style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Paste one URL per line. Impulse will create useful memories from each source.", style = MaterialTheme.typography.bodySmall, color = Hint)
                OutlinedTextField(name, { name = it }, label = { Text("Collection name") }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text("What will you use this for?") }, minLines = 2)
                OutlinedTextField(urlsText, { urlsText = it }, label = { Text("URLs") }, minLines = 4)
                Text("${urls.size} valid link${if (urls.size == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall, color = Hint)
            }
        },
        confirmButton = {
            Button(
                enabled = !creating && name.isNotBlank() && urls.isNotEmpty(),
                onClick = { onCreate(name.trim(), description.trim().ifBlank { null }, urls) },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                if (creating) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Create memories")
            }
        },
        dismissButton = { TextButton(enabled = !creating, onClick = onDismiss) { Text("Cancel") } },
        containerColor = Surface
    )
}

@Composable
private fun EmptyMemoryState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape).background(NeutralContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Primary)
        }
        Spacer(Modifier.height(14.dp))
        Text("Add your first source", style = MaterialTheme.typography.titleLarge)
        Text(
            "Share a video, article, recipe, or product with Impulse.",
            style = MaterialTheme.typography.bodySmall,
            color = Hint
        )
    }
}

@Composable
private fun ImpulseBottomBar(onUseMemories: () -> Unit) {
    NavigationBar(containerColor = Surface, modifier = Modifier.navigationBarsPadding()) {
        NavigationBarItem(
            selected = true,
            onClick = {},
            icon = { Icon(Icons.Default.Home, "Home") },
            label = { Text("Home") },
            colors = impulseNavigationColors()
        )
        NavigationBarItem(
            selected = false,
            onClick = {},
            icon = { Icon(Icons.Default.Search, "Memories") },
            label = { Text("Memories") },
            colors = impulseNavigationColors()
        )
        NavigationBarItem(
            selected = false,
            onClick = onUseMemories,
            icon = { Icon(Icons.Default.AutoAwesome, "Plan") },
            label = { Text("Plan") },
            colors = impulseNavigationColors()
        )
    }
}

@Composable
private fun impulseNavigationColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Primary,
    selectedTextColor = Primary,
    indicatorColor = AccentContainer,
    unselectedIconColor = Hint,
    unselectedTextColor = Hint
)
