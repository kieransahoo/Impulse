package com.impulse.ui.main

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.impulse.data.model.CollectionSourceResponse
import com.impulse.data.model.GroundingMemoryResponse
import com.impulse.data.model.MemoryResponse
import com.impulse.data.model.PlanStepResponse
import com.impulse.data.model.SavedPlanResponse
import com.impulse.data.model.SavedPlanStepResponse
import com.impulse.data.model.UserCollectionResponse
import com.impulse.data.model.UserSession
import com.impulse.ui.theme.AccentContainer
import com.impulse.ui.theme.DividerColor
import com.impulse.ui.theme.ErrorColor
import com.impulse.ui.theme.ErrorContainer
import com.impulse.ui.theme.Hint
import com.impulse.ui.theme.Ink
import com.impulse.ui.theme.NeutralContainer
import com.impulse.ui.theme.Paper
import com.impulse.ui.theme.Primary
import com.impulse.ui.theme.Secondary
import com.impulse.ui.theme.SuccessColor
import com.impulse.ui.theme.SuccessContainer
import com.impulse.ui.theme.Surface
import com.impulse.ui.theme.SurfaceBright
import com.impulse.ui.component.NewCollectionAction
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    session: UserSession,
    onSignOut: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var showCapture by remember { mutableStateOf(false) }

    LaunchedEffect(session.userId) { viewModel.start(session.userId) }
    LaunchedEffect(state.error, state.message) {
        if (state.error != null || state.message != null) {
            delay(2_000)
            viewModel.clearFeedback()
        }
    }
    BackHandler(
        enabled = state.activePlan != null ||
            state.activeSavedPlan != null ||
            state.navigationHistory.isNotEmpty()
    ) {
        viewModel.back()
    }

    Scaffold(
        containerColor = Paper,
        topBar = {
            AppHeader(
                session = session,
                refreshing = state.refreshing,
                showBack = state.destination == MainDestination.PLANS &&
                    (state.activePlan != null || state.activeSavedPlan != null),
                onBack = viewModel::closePlan,
                onRefresh = viewModel::refresh
            )
        },
        bottomBar = {
            AppBottomBar(state.destination, viewModel::select)
        },
        floatingActionButton = {
            if (state.destination != MainDestination.PROFILE) {
                FloatingActionButton(
                    onClick = { showCapture = true },
                    containerColor = Primary,
                    contentColor = Color.White,
                    shape = CircleShape
                ) { Icon(Icons.Default.Add, contentDescription = "Save a link") }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> LoadingScreen()
                state.destination == MainDestination.HOME -> HomePage(
                    session = session,
                    state = state,
                    onAdd = { showCapture = true },
                    onPlan = viewModel::createPlan,
                    onTogglePlanCollection = viewModel::togglePlanCollection,
                    onClearPlanCollections = viewModel::clearPlanCollections,
                    onDestination = viewModel::select,
                    onOpenPlan = viewModel::openPlan
                )
                state.destination == MainDestination.MEMORIES -> MemoriesPage(
                    state = state,
                    onSearch = viewModel::search,
                    onClearSearch = viewModel::clearSearch,
                    onAdd = { showCapture = true },
                    onDeleteMemory = viewModel::deleteMemory,
                    onChangeCollection = viewModel::changeMemoryCollection,
                    onCreateCollection = viewModel::createCollectionForMemory,
                    onUpdateCollection = viewModel::updateCollection,
                    onDeleteCollection = viewModel::deleteCollection,
                    onRemoveSource = viewModel::removeSource
                )
                state.destination == MainDestination.PLANS -> PlansPage(
                    state = state,
                    onCreate = viewModel::createPlan,
                    onTogglePlanCollection = viewModel::togglePlanCollection,
                    onClearPlanCollections = viewModel::clearPlanCollections,
                    onCreateStarter = viewModel::createStarterPlan,
                    onUseAllMemories = viewModel::retryPlanWithAllMemories,
                    onRegenerate = viewModel::regenerateSavedPlan,
                    onActivate = viewModel::activateSavedPlan,
                    onComplete = viewModel::completeSavedPlan,
                    onDeletePlan = viewModel::deleteSavedPlan,
                    onSave = viewModel::saveActivePlan,
                    onOpen = viewModel::openPlan,
                    onToggleStep = viewModel::toggleStep
                )
                else -> ProfilePage(
                    session = session,
                    state = state,
                    onSignOut = onSignOut,
                    onClearMemories = viewModel::clearAllMemories,
                    onUpdatePersonalization = viewModel::updatePersonalization
                )
            }

            state.error?.let {
                FeedbackBanner(it, true, Modifier.align(Alignment.TopCenter), viewModel::clearFeedback)
            }
            state.message?.let {
                FeedbackBanner(it, false, Modifier.align(Alignment.TopCenter), viewModel::clearFeedback)
            }
        }
    }

    if (showCapture) {
        SaveLinkSheet(
            collections = state.collections,
            saving = state.savingSource,
            error = state.error,
            onDismiss = { if (!state.savingSource) showCapture = false },
            onSave = { url, note, collectionId ->
                viewModel.saveSource(url, note, collectionId) { showCapture = false }
            },
            onCreateAndSave = { url, note, collectionName ->
                viewModel.saveSourceToNewCollection(url, note, collectionName) {
                    showCapture = false
                }
            }
        )
    }
}

@Composable
private fun AppHeader(
    session: UserSession,
    refreshing: Boolean,
    showBack: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    Surface(color = Paper.copy(alpha = .96f), tonalElevation = 0.dp) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 18.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to plans")
                }
                Text("Plan", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            } else {
                BrandMark()
                Spacer(Modifier.width(10.dp))
                Text("Impulse", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            }
            Text(
                session.displayName.substringBefore(" "),
                color = Hint,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
            IconButton(onClick = onRefresh, enabled = !refreshing) {
                if (refreshing) CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp, color = Primary)
                else Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Hint)
            }
        }
    }
}

@Composable
private fun BrandMark(modifier: Modifier = Modifier) {
    Box(
        modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Ink),
        contentAlignment = Alignment.Center
    ) {
        Text("I", color = Color.White, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, fontSize = 19.sp)
    }
}

@Composable
private fun AppBottomBar(selected: MainDestination, onSelect: (MainDestination) -> Unit) {
    NavigationBar(containerColor = Surface, modifier = Modifier.navigationBarsPadding()) {
        val destinations = listOf(
            Triple(MainDestination.HOME, Icons.Default.Home, "Home"),
            Triple(MainDestination.MEMORIES, Icons.Default.Search, "Memories"),
            Triple(MainDestination.PLANS, Icons.Default.AutoAwesome, "Plans"),
            Triple(MainDestination.PROFILE, Icons.Default.Person, "Profile")
        )
        destinations.forEach { (destination, icon, label) ->
            NavigationBarItem(
                selected = destination == selected,
                onClick = { onSelect(destination) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Ink,
                    selectedTextColor = Ink,
                    indicatorColor = Secondary,
                    unselectedIconColor = Hint,
                    unselectedTextColor = Hint
                )
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        repeat(4) { index ->
            Box(
                Modifier.fillMaxWidth().height(if (index == 0) 130.dp else 82.dp)
                    .background(NeutralContainer, RoundedCornerShape(20.dp))
            )
        }
    }
}

@Composable
private fun HomePage(
    session: UserSession,
    state: MainUiState,
    onAdd: () -> Unit,
    onPlan: (String) -> Unit,
    onTogglePlanCollection: (String) -> Unit,
    onClearPlanCollections: () -> Unit,
    onDestination: (MainDestination) -> Unit,
    onOpenPlan: (SavedPlanResponse) -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.takeIf(String::isNotBlank)
                ?.let { spoken ->
                    prompt = listOf(prompt.trim(), spoken.trim())
                        .filter(String::isNotBlank)
                        .joinToString(" ")
                }
        }
    }
    fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your plan query")
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "Voice input is not available on this device.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    val activePlans = state.plans.filter { it.status == "ACTIVE" }
    val activePlanIds = activePlans.mapTo(mutableSetOf(), SavedPlanResponse::id)
    val recentPlans = state.plans.filterNot { it.id in activePlanIds }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            EditorialLabel("YOUR USEFUL INTERNET")
            Text(
                "Good to see you,\n${session.displayName.substringBefore(" ")}.",
                style = MaterialTheme.typography.displaySmall
            )
        }
        item {
            Surface(
                color = Surface,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, DividerColor.copy(alpha = .65f)),
                tonalElevation = 1.dp,
                shadowElevation = 3.dp
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("What do you want to do?", color = Ink, style = MaterialTheme.typography.titleLarge)
                    Text("Create a plan grounded in what you saved.", color = Hint, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        placeholder = { Text("Plan a focused evening…", color = Hint) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        trailingIcon = {
                            IconButton(onClick = ::startVoiceInput) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "Speak plan query",
                                    tint = Primary
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            keyboard?.hide()
                            if (prompt.isNotBlank()) onPlan(prompt)
                        }),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Ink,
                            unfocusedTextColor = Ink,
                            focusedContainerColor = SurfaceBright,
                            unfocusedContainerColor = SurfaceBright,
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = DividerColor
                        )
                    )
                    PlanCollectionSelector(
                        collections = state.collections,
                        memories = state.memories,
                        selectedIds = state.selectedPlanCollectionIds,
                        onToggle = onTogglePlanCollection,
                        onClear = onClearPlanCollections
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { onPlan(prompt) },
                        enabled = prompt.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Create personalized plan"); Spacer(Modifier.weight(1f)); Text("→", fontSize = 20.sp) }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricStat("${state.memories.size}", "Memories", Modifier.weight(1f))
                MetricDivider()
                MetricStat("${state.collections.size}", "Collections", Modifier.weight(1f))
                MetricDivider()
                MetricStat("${state.plans.size}", "Plans", Modifier.weight(1f))
            }
        }
        if (activePlans.isNotEmpty()) {
            item {
                SectionTitle(
                    "IN PROGRESS",
                    if (activePlans.size == 1) "Active plan" else "Active plans",
                    null,
                    null
                )
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 20.dp)
                ) {
                    items(activePlans, key = SavedPlanResponse::id) { plan ->
                        ActivePlanCard(
                            plan = plan,
                            onOpen = onOpenPlan,
                            modifier = Modifier.width(286.dp)
                        )
                    }
                }
            }
        }
        item {
            SectionTitle("RECENTLY SAVED", "Your memories", "View all") { onDestination(MainDestination.MEMORIES) }
        }
        if (state.memories.isEmpty()) {
            item { EmptyState(Icons.Default.Link, "No memories yet", "Save one useful link to begin.", "Save a link", onAdd) }
        } else {
            items(state.memories.take(4), key = { it.id }) { MemoryRow(it) }
        }
        if (state.plans.isEmpty()) {
            item {
                SectionTitle("CONTINUE", "Saved plans", "View all") { onDestination(MainDestination.PLANS) }
            }
            item { EmptyState(Icons.Default.AutoAwesome, "No plans yet", "Create a plan from your saved knowledge.", null, null) }
        } else if (recentPlans.isNotEmpty()) {
            item {
                SectionTitle("SAVED", "Other plans", "View all") { onDestination(MainDestination.PLANS) }
            }
            items(recentPlans.take(3), key = { it.id }) { SavedPlanCard(it, onOpenPlan) }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun ActivePlanCard(
    plan: SavedPlanResponse,
    onOpen: (SavedPlanResponse) -> Unit,
    modifier: Modifier = Modifier
) {
    val completed = plan.plan.count(SavedPlanStepResponse::completed)
    val total = plan.plan.size
    Surface(
        modifier = modifier.clickable { onOpen(plan) },
        color = Surface,
        contentColor = Ink,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, DividerColor.copy(alpha = .65f)),
        tonalElevation = 1.dp,
        shadowElevation = 3.dp
    ) {
        Column(Modifier.padding(17.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "IN PROGRESS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text("$completed of $total", style = MaterialTheme.typography.labelMedium)
            }
            Text(
                plan.goal,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 7.dp)
            )
            Text(
                "Continue plan →",
                style = MaterialTheme.typography.bodySmall,
                color = Hint,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun MetricStat(value: String, label: String, modifier: Modifier) {
    Column(
        modifier.padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontFamily = FontFamily.Serif, fontSize = 25.sp, fontWeight = FontWeight.Medium)
        Text(label, style = MaterialTheme.typography.labelMedium, color = Hint)
    }
}

@Composable
private fun MetricDivider() {
    Box(Modifier.width(1.dp).height(34.dp).background(DividerColor))
}

@Composable
private fun MemoriesPage(
    state: MainUiState,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onAdd: () -> Unit,
    onDeleteMemory: (String) -> Unit,
    onChangeCollection: (MemoryResponse, String?) -> Unit,
    onCreateCollection: (MemoryResponse, String) -> Unit,
    onUpdateCollection: (String, String, String?) -> Unit,
    onDeleteCollection: (String) -> Unit,
    onRemoveSource: (String, String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All categories") }
    var selectedCollectionId by remember { mutableStateOf<String?>(null) }
    var selectedCollection by remember { mutableStateOf<UserCollectionResponse?>(null) }
    val keyboard = LocalSoftwareKeyboardController.current
    val memories = state.searchResults ?: state.memories
    val categories = state.memories
        .map(MemoryResponse::category)
        .filter(String::isNotBlank)
        .distinct()
    val collections = state.collections.filterNot { it.name.equals("ALL", true) }
    val collectionMemoryIds = selectedCollectionId?.let { collectionId ->
        state.collections
            .firstOrNull { it.id == collectionId }
            ?.sources
            ?.mapNotNullTo(mutableSetOf(), CollectionSourceResponse::memoryId)
            .orEmpty()
    }
    val filtered = memories.filter { memory ->
        (selectedCategory == "All categories" || memory.category == selectedCategory) &&
            (collectionMemoryIds == null || memory.id in collectionMemoryIds)
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { LargeTitle("MEMORY LIBRARY", "Memories", "Browse, search, and organize what you saved.") }
        item {
            OutlinedTextField(
                query, {
                    query = it
                    if (it.isBlank()) onClearSearch()
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search saved knowledge") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotBlank()) IconButton(onClick = { query = ""; onClearSearch() }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (query.isNotBlank()) onSearch(query)
                    keyboard?.hide()
                }),
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors()
            )
            Button(
                onClick = {
                    onSearch(query)
                    keyboard?.hide()
                },
                enabled = query.isNotBlank() && !state.searching,
                modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (state.searching) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Search memories")
            }
        }
        item {
            AutoCategoryDropdown(
                categories = categories,
                selected = selectedCategory,
                onSelect = { selectedCategory = it }
            )
        }
        item {
            SectionTitle("COLLECTIONS", "Your collections", null, null)
            LazyRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    MemoryFilterPill("All memories", selectedCollectionId == null, null) {
                        selectedCollectionId = null
                    }
                }
                items(collections, key = { "collection:${it.id}" }) { collection ->
                    CollectionFilterPill(
                        collection = collection,
                        memories = state.memories,
                        selected = selectedCollectionId == collection.id,
                        onSelect = {
                            selectedCollectionId = collection.id
                        },
                        onManage = { selectedCollection = collection }
                    )
                }
            }
        }
        item { SectionTitle(if (state.searchResults == null) "ALL MEMORIES" else "SEARCH RESULTS", "${filtered.size} ready", null, null) }
        if (filtered.isEmpty()) {
            item { EmptyState(Icons.Default.Bookmark, "Nothing here yet", "Save a source or try another search.", "Save a link", onAdd) }
        } else {
            items(
                filtered.chunked(2),
                key = { row -> row.joinToString("-") { it.id } }
            ) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { memory ->
                        MemoryCard(
                            memory = memory,
                            collections = state.collections,
                            onDelete = onDeleteMemory,
                            onChangeCollection = onChangeCollection,
                            onCreateCollection = onCreateCollection,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }

    selectedCollection?.let { collection ->
        CollectionDialog(
            collection = collection,
            onDismiss = { selectedCollection = null },
            onUpdate = { name, description ->
                onUpdateCollection(collection.id, name, description)
                selectedCollection = null
            },
            onDelete = {
                onDeleteCollection(collection.id)
                selectedCollection = null
            },
            onRemoveSource = { onRemoveSource(collection.id, it) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoCategoryDropdown(
    categories: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("All categories") + categories
    Column {
        Text("AUTO-GENERATED FILTER", style = MaterialTheme.typography.labelSmall, color = Primary)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth().padding(top = 7.dp)
        ) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                leadingIcon = { FilterThumbnail(null, selected) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),
                shape = RoundedCornerShape(15.dp),
                colors = fieldColors()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilterThumbnail(null, option)
                                Text(option, Modifier.padding(start = 10.dp))
                            }
                        },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectionFilterPill(
    collection: UserCollectionResponse,
    memories: List<MemoryResponse>,
    selected: Boolean,
    onSelect: () -> Unit,
    onManage: () -> Unit
) {
    Surface(
        color = if (selected) Ink else AccentContainer,
        contentColor = if (selected) Color.White else Ink,
        shape = CircleShape,
        border = BorderStroke(1.dp, if (selected) Ink else DividerColor)
    ) {
        Row(
            modifier = Modifier.clickable(onClick = onSelect),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterThumbnail(
                thumbnailUrl = collectionThumbnail(collection, memories),
                placeholder = collection.name,
                modifier = Modifier.padding(start = 5.dp)
            )
            Text(
                collection.name,
                Modifier.padding(horizontal = 7.dp, vertical = 9.dp),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
            if (selected) {
                IconButton(onClick = onManage, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Manage ${collection.name} collection",
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Spacer(Modifier.width(5.dp))
            }
        }
    }
}

@Composable
private fun MemoryFilterPill(
    label: String,
    selected: Boolean,
    thumbnailUrl: String?,
    thumbnailSize: androidx.compose.ui.unit.Dp = 38.dp,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = if (selected) Ink else Surface,
        contentColor = if (selected) Color.White else Hint,
        shape = CircleShape,
        border = BorderStroke(1.dp, if (selected) Ink else DividerColor)
    ) {
        Row(
            modifier = Modifier.padding(start = 5.dp, end = 13.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterThumbnail(thumbnailUrl, label, size = thumbnailSize)
            Text(
                label,
                Modifier.padding(start = 7.dp),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FilterThumbnail(
    thumbnailUrl: String?,
    placeholder: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 38.dp
) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = AccentContainer,
        contentColor = Primary
    ) {
        if (thumbnailUrl != null) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    placeholder.take(1).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun collectionThumbnail(
    collection: UserCollectionResponse,
    memories: List<MemoryResponse>
): String? = collection.sources.firstNotNullOfOrNull { source ->
    memories.firstOrNull { it.id == source.memoryId }?.thumbnailUrl
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = if (selected) Ink else Surface,
        contentColor = if (selected) Color.White else Hint,
        shape = CircleShape,
        border = BorderStroke(1.dp, if (selected) Ink else DividerColor)
    ) { Text(label, Modifier.padding(horizontal = 14.dp, vertical = 9.dp), style = MaterialTheme.typography.bodySmall) }
}

@Composable
private fun CompactCollectionTile(
    collection: UserCollectionResponse,
    memories: List<MemoryResponse>,
    onClick: () -> Unit
) {
    val thumbnail = collection.sources.firstNotNullOfOrNull { source ->
        memories.firstOrNull { it.id == source.memoryId }?.thumbnailUrl
    }
    Column(
        modifier = Modifier.width(78.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(58.dp),
            color = AccentContainer,
            contentColor = Primary,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, if (collection.name.equals("ALL", true)) Ink else DividerColor)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (thumbnail != null) {
                    AsyncImage(
                        model = thumbnail,
                        contentDescription = "${collection.name} collection thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        collection.name.take(1).uppercase(),
                        fontFamily = FontFamily.Serif,
                        fontSize = 24.sp,
                        color = Primary
                    )
                }
            }
        }
        Text(
            collection.name,
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MemoryCard(
    memory: MemoryResponse,
    collections: List<UserCollectionResponse>,
    onDelete: (String) -> Unit,
    onChangeCollection: (MemoryResponse, String?) -> Unit,
    onCreateCollection: (MemoryResponse, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    var showCollectionPicker by remember { mutableStateOf(false) }
    val openOriginal = {
        context.startActivity(Intent(Intent.ACTION_VIEW, memory.sourceUrl.toUri()))
    }
    Card(
        modifier = modifier.clickable(onClick = openOriginal),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(19.dp),
        border = BorderStroke(1.dp, DividerColor)
    ) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(1.18f)) {
                if (memory.thumbnailUrl != null) {
                    AsyncImage(
                        memory.thumbnailUrl,
                        "${memory.title} thumbnail",
                        Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        Modifier.fillMaxSize().background(AccentContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            memory.title.take(1),
                            color = Primary,
                            fontFamily = FontFamily.Serif,
                            fontSize = 46.sp
                        )
                    }
                }
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(7.dp),
                    color = Ink.copy(alpha = .82f),
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Row(
                        Modifier.padding(7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open original",
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
            Column(Modifier.padding(11.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        memory.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = SuccessColor,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(
                        onClick = { showCollectionPicker = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = "Change collection", tint = Primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { confirmDelete = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Remove memory", tint = ErrorColor, modifier = Modifier.size(18.dp))
                    }
                }
                Text(
                    memory.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    collections.firstOrNull { collection ->
                        collection.sources.any { it.memoryId == memory.id }
                    }?.name ?: "No collection",
                    style = MaterialTheme.typography.bodySmall,
                    color = Hint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Remove this memory?") },
            text = { Text("It will be removed from your library and collections. Saved plans will remain.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        onDelete(memory.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorColor)
                ) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
            containerColor = Surface
        )
    }
    if (showCollectionPicker) {
        MemoryCollectionSheet(
            memory = memory,
            collections = collections,
            onDismiss = { showCollectionPicker = false },
            onSelect = {
                showCollectionPicker = false
                onChangeCollection(memory, it)
            },
            onCreateNew = {
                showCollectionPicker = false
                onCreateCollection(memory, it)
            },
        )
    }
}

@Composable
private fun MemoryRow(memory: MemoryResponse) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.clickable {
            context.startActivity(Intent(Intent.ACTION_VIEW, memory.sourceUrl.toUri()))
        },
        color = Surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DividerColor)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (memory.thumbnailUrl != null) AsyncImage(memory.thumbnailUrl, null, Modifier.size(64.dp).clip(RoundedCornerShape(13.dp)), contentScale = ContentScale.Crop)
            else Box(Modifier.size(64.dp).background(AccentContainer, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                Text(memory.title.take(1), color = Primary, fontFamily = FontFamily.Serif, fontSize = 26.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(memory.category.uppercase(), style = MaterialTheme.typography.labelSmall, color = SuccessColor)
                Text(memory.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(memory.summary, style = MaterialTheme.typography.bodySmall, color = Hint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Open original source",
                tint = Hint,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemoryCollectionSheet(
    memory: MemoryResponse,
    collections: List<UserCollectionResponse>,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit,
    onCreateNew: (String) -> Unit
) {
    val all = collections.firstOrNull { it.name.equals("ALL", true) }
    val currentId = collections.firstOrNull { collection ->
        collection.sources.any { it.memoryId == memory.id }
    }?.id
    var creatingNew by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Change collection",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                NewCollectionAction(creatingNew) { creatingNew = !creatingNew }
            }
            Text(
                memory.title,
                style = MaterialTheme.typography.bodySmall,
                color = Hint,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (creatingNew) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it.take(200) },
                    label = { Text("Collection name") },
                    placeholder = { Text("Weekend ideas") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (newName.trim().length >= 2) {
                            keyboard?.hide()
                            onCreateNew(newName.trim())
                        }
                    }),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors()
                )
                Button(
                    onClick = {
                        keyboard?.hide()
                        onCreateNew(newName.trim())
                    },
                    enabled = newName.trim().length >= 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Create and move") }
                HorizontalDivider(Modifier.padding(vertical = 5.dp), color = DividerColor)
            }
            collections
                .sortedByDescending { it.name.equals("ALL", true) }
                .forEach { collection ->
                    CollectionChoice(
                        name = collection.name,
                        description = if (collection.name.equals("ALL", true)) {
                            "Default collection"
                        } else {
                            "${collection.processedSources} memories"
                        },
                        selected = currentId == collection.id,
                        onClick = {
                            onSelect(if (collection.id == all?.id) null else collection.id)
                        }
                    )
                }
        }
    }
}

@Composable
private fun PlanCollectionSelector(
    collections: List<UserCollectionResponse>,
    memories: List<MemoryResponse>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    onClear: () -> Unit
) {
    val selectable = collections.filterNot { it.name.equals("ALL", true) }
    if (selectable.isEmpty()) return
    Column(Modifier.padding(top = 12.dp)) {
        Text(
            "PLAN FROM",
            style = MaterialTheme.typography.labelSmall,
            color = Primary
        )
        LazyRow(
            modifier = Modifier.padding(top = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            item {
                MemoryFilterPill(
                    label = "All memories",
                    selected = selectedIds.isEmpty(),
                    thumbnailUrl = null,
                    thumbnailSize = 30.dp,
                    onClick = onClear
                )
            }
            items(selectable, key = UserCollectionResponse::id) { collection ->
                MemoryFilterPill(
                    label = collection.name,
                    selected = collection.id in selectedIds,
                    thumbnailUrl = collectionThumbnail(collection, memories),
                    thumbnailSize = 30.dp,
                    onClick = { onToggle(collection.id) }
                )
            }
        }
        Text(
            if (selectedIds.isEmpty()) {
                "Relevant memories can be used from every collection."
            } else {
                "Only relevant memories from the selected collections will be used."
            },
            style = MaterialTheme.typography.labelSmall,
            color = Hint,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun PlansPage(
    state: MainUiState,
    onCreate: (String) -> Unit,
    onTogglePlanCollection: (String) -> Unit,
    onClearPlanCollections: () -> Unit,
    onCreateStarter: () -> Unit,
    onUseAllMemories: () -> Unit,
    onRegenerate: (String) -> Unit,
    onActivate: () -> Unit,
    onComplete: () -> Unit,
    onDeletePlan: () -> Unit,
    onSave: () -> Unit,
    onOpen: (SavedPlanResponse) -> Unit,
    onToggleStep: (Int) -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.takeIf(String::isNotBlank)
                ?.let { spoken ->
                    prompt = listOf(prompt.trim(), spoken.trim())
                        .filter(String::isNotBlank)
                        .joinToString(" ")
                }
        }
    }
    fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your plan query")
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "Voice input is not available on this device.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    if (state.activePlan != null || state.activeSavedPlan != null) {
        PlanDetail(
            state,
            onSave,
            onToggleStep,
            onCreateStarter,
            onUseAllMemories,
            onRegenerate,
            onActivate,
            onComplete,
            onDeletePlan
        )
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { LargeTitle("FROM MEMORY TO ACTION", "Plans", "Turn your saved knowledge into a clear next step.") }
        item {
            Surface(
                color = Surface,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, DividerColor.copy(alpha = .65f)),
                tonalElevation = 1.dp,
                shadowElevation = 3.dp
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Create a grounded plan", color = Ink, style = MaterialTheme.typography.titleMedium)
                    Text("Use the memories most relevant to your goal.", color = Hint, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        prompt, { prompt = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("What do you want to do?", color = Hint) },
                        trailingIcon = {
                            IconButton(onClick = ::startVoiceInput) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "Speak plan query",
                                    tint = Primary
                                )
                            }
                        },
                        minLines = 2,
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            keyboard?.hide()
                            if (prompt.isNotBlank() && !state.planning) onCreate(prompt)
                        }),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Ink, unfocusedTextColor = Ink,
                            focusedContainerColor = SurfaceBright, unfocusedContainerColor = SurfaceBright,
                            focusedBorderColor = Primary, unfocusedBorderColor = DividerColor
                        )
                    )
                    PlanCollectionSelector(
                        collections = state.collections,
                        memories = state.memories,
                        selectedIds = state.selectedPlanCollectionIds,
                        onToggle = onTogglePlanCollection,
                        onClear = onClearPlanCollections
                    )
                    Button(
                        onClick = {
                            keyboard?.hide()
                            onCreate(prompt)
                        },
                        enabled = prompt.isNotBlank() && !state.planning,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (state.planning) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("Create personalized plan")
                    }
                    if (state.planning) Text("Finding memories → Building your plan", color = Primary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 10.dp))
                }
            }
        }
        item { SectionTitle("SAVED FOR LATER", "Your plans", null, null) }
        if (state.plans.isEmpty()) item { EmptyState(Icons.Default.AutoAwesome, "No saved plans", "Create a plan and save it here.", null, null) }
        else items(state.plans, key = { it.id }) { SavedPlanCard(it, onOpen) }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PlanDetail(
    state: MainUiState,
    onSave: () -> Unit,
    onToggleStep: (Int) -> Unit,
    onCreateStarter: () -> Unit,
    onUseAllMemories: () -> Unit,
    onRegenerate: (String) -> Unit,
    onActivate: () -> Unit,
    onComplete: () -> Unit,
    onDeletePlan: () -> Unit
) {
    val generated = state.activePlan
    val saved = state.activeSavedPlan
    val goal = generated?.goal ?: saved!!.goal
    val explanation = generated?.explanation ?: saved!!.explanation
    val generatedSteps = generated?.plan
    val savedSteps = saved?.plan
    var showUpdatePlan by remember { mutableStateOf(false) }
    var showDeletePlan by remember { mutableStateOf(false) }
    var showSources by remember { mutableStateOf(false) }
    var regenerationGoal by remember(saved?.id) {
        mutableStateOf(saved?.goal.orEmpty())
    }
    val grounding = generated?.groundingMemories ?: saved?.retrievedMemoryIds.orEmpty().mapNotNull { id ->
        state.memories.find { it.id == id }?.let {
            GroundingMemoryResponse(
                id = it.id,
                title = it.title,
                summary = it.summary,
                sourceUrl = it.sourceUrl,
                thumbnailUrl = it.thumbnailUrl,
                platform = it.platform ?: it.category
            )
        }
    }
    val exportPlan = ExportPlan(
        goal = goal,
        explanation = explanation,
        steps = generatedSteps?.map {
            ExportPlanStep(it.step, it.durationMinutes, it.reason)
        } ?: savedSteps.orEmpty().map {
            ExportPlanStep(it.step, it.durationMinutes, it.reason)
        },
        sourceCount = generated?.retrievedMemoryIds?.size ?: saved?.retrievedMemoryIds.orEmpty().size
    )
    val context = LocalContext.current
    val exportScope = rememberCoroutineScope()
    fun shareExport(uri: Uri, mimeType: String, chooserTitle: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = android.content.ClipData.newUri(
                context.contentResolver,
                chooserTitle,
                uri
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
    }
    fun writeExport(
        uri: Uri,
        bytes: ByteArray,
        success: String,
        mimeType: String,
        chooserTitle: String
    ) {
        exportScope.launch {
            val completed = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                        ?: error("Could not open the selected file.")
                }.isSuccess
            }
            Toast.makeText(
                context,
                if (completed) success else "Could not export this plan.",
                Toast.LENGTH_SHORT
            ).show()
            if (completed) {
                shareExport(uri, mimeType, chooserTitle)
            }
        }
    }
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let {
            writeExport(
                it,
                exportPlan.toPdf(),
                "PDF exported.",
                "application/pdf",
                "Share plan PDF"
            )
        }
    }
    val savedAlready = saved != null || state.plans.any { it.goal == goal && it.explanation == explanation }
    val needsStarterChoice = generated?.groundingStatus == "NO_GROUNDING" &&
        generated.plan.isEmpty()
    val collectionScoped = state.selectedPlanCollectionIds.isNotEmpty()
    val availableCollectionIds = state.collections
        .filterNot { it.name.equals("ALL", true) }
        .mapTo(mutableSetOf(), UserCollectionResponse::id)
    val usingAllCollections = state.selectedPlanCollectionIds.isEmpty() ||
        (
            availableCollectionIds.isNotEmpty() &&
                state.selectedPlanCollectionIds.containsAll(availableCollectionIds)
            )
    val allSavedStepsCompleted = savedSteps?.isNotEmpty() == true &&
        savedSteps.all(SavedPlanStepResponse::completed)
    val planHeaderColor = when (saved?.status) {
        "SAVED" -> Surface
        "ACTIVE" -> Secondary
        "COMPLETED" -> SuccessContainer
        else -> AccentContainer
    }
    val planHeaderBorder = if (saved?.status == "SAVED") {
        BorderStroke(1.dp, DividerColor)
    } else {
        null
    }
    if (showUpdatePlan && saved != null) {
        AlertDialog(
            onDismissRequest = { if (!state.planning) showUpdatePlan = false },
            title = { Text("Update this plan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Edit the goal, then rebuild this saved plan with your latest memories.")
                    OutlinedTextField(
                        value = regenerationGoal,
                        onValueChange = { regenerationGoal = it.take(2_000) },
                        label = { Text("Plan goal") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRegenerate(regenerationGoal)
                        showUpdatePlan = false
                    },
                    enabled = regenerationGoal.isNotBlank() && !state.planning
                ) { Text("Replace plan") }
            },
            dismissButton = {
                TextButton(onClick = { showUpdatePlan = false }) { Text("Cancel") }
            }
        )
    }
    if (showDeletePlan && saved != null) {
        AlertDialog(
            onDismissRequest = { showDeletePlan = false },
            title = { Text("Delete this plan?") },
            text = { Text("This removes the plan and its progress. Your memories stay saved.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeletePlan = false
                        onDeletePlan()
                    }
                ) { Text("Delete", color = ErrorColor) }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePlan = false }) { Text("Cancel") }
            }
        )
    }
    if (showSources) {
        ModalBottomSheet(
            onDismissRequest = { showSources = false },
            containerColor = Paper
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EditorialLabel("SOURCES USED")
                Text(
                    "${grounding.size} saved ${if (grounding.size == 1) "memory" else "memories"}",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    "These memories were used to ground this plan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Hint
                )
                grounding.forEach { GroundingCard(it) }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(
                color = planHeaderColor,
                shape = RoundedCornerShape(24.dp),
                border = planHeaderBorder
            ) {
                Column(Modifier.padding(22.dp)) {
                    EditorialLabel(
                        when {
                            saved?.status == "SAVED" -> "SAVED"
                            saved?.status == "ACTIVE" -> "IN PROGRESS"
                            saved?.status == "COMPLETED" -> "COMPLETED"
                            generated?.groundingStatus == "STRONG_GROUNDING" ->
                                "PERSONALIZED PLAN"
                            generated?.groundingStatus == "PARTIAL_GROUNDING" ->
                                "PERSONALIZED PLAN"
                            generated?.groundingStatus == "NO_GROUNDING" ->
                                "NO MATCHING SAVED MEMORIES"
                            else -> "PLAN"
                        }
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            goal,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (saved != null) {
                            IconButton(onClick = { showUpdatePlan = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit and update plan")
                            }
                            IconButton(onClick = { showDeletePlan = true }) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Delete plan",
                                    tint = ErrorColor
                                )
                            }
                        }
                    }
                    Text(explanation, style = MaterialTheme.typography.bodyMedium, color = Ink.copy(alpha = .72f))
                    val count = generatedSteps?.size ?: savedSteps.orEmpty().size
                    if (saved?.status == "ACTIVE" || saved?.status == "COMPLETED") {
                        Text(
                            "${state.completedSteps.size} of $count steps complete",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                        LinearProgressIndicator(
                            progress = {
                                if (count == 0) 0f
                                else state.completedSteps.size.toFloat() / count
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 7.dp)
                                .height(6.dp)
                                .clip(CircleShape),
                            color = SuccessColor,
                            trackColor = Ink.copy(alpha = .10f)
                        )
                    }
                    if (!needsStarterChoice) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = { pdfLauncher.launch("${safeFileName(goal)}.pdf") }
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("PDF")
                            }
                            if (grounding.isNotEmpty()) {
                                TextButton(onClick = { showSources = true }) {
                                    Icon(
                                        Icons.Default.Bookmark,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text("Sources used · ${grounding.size}")
                                }
                            }
                        }
                    }
                    if (!savedAlready && !needsStarterChoice) Button(onClick = onSave, enabled = !state.savingPlan, modifier = Modifier.padding(top = 6.dp), colors = ButtonDefaults.buttonColors(containerColor = Ink)) {
                        if (state.savingPlan) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp) else Text("Save plan")
                    }
                    if (saved != null) {
                        when (saved.status) {
                            "SAVED" -> Button(
                                onClick = onActivate,
                                enabled = !state.updatingPlanProgress,
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Start plan")
                            }
                            "ACTIVE" -> {
                                if (allSavedStepsCompleted) {
                                    Button(
                                        onClick = onComplete,
                                        enabled = !state.updatingPlanProgress,
                                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessColor),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text("Complete plan")
                                    }
                                }
                            }
                            "COMPLETED" -> Unit
                        }
                    }
                }
            }
        }
        if (needsStarterChoice) {
            item {
                Surface(
                    color = Surface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, DividerColor)
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("No relevant memories found", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (collectionScoped) {
                                "The selected collections do not contain content that supports this goal."
                            } else {
                                "Your saved memories do not contain content that supports this goal."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Hint
                        )
                        if (generated.missingContext.isNotEmpty()) {
                            Text(
                                "Helpful details: ${generated.missingContext.joinToString()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Hint
                            )
                        }
                        if (generated.suggestedSources.isNotEmpty()) {
                            Text(
                                "Save next: ${generated.suggestedSources.joinToString()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Hint
                            )
                        }
                        if (!usingAllCollections) {
                            Button(
                                onClick = onUseAllMemories,
                                enabled = !state.planning,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                if (state.planning) {
                                    CircularProgressIndicator(
                                        Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Search all memories")
                                }
                            }
                        }
                        Text(
                            if (usingAllCollections) {
                                "No relevant memory was found across your collections. Add more relevant memories, then try again."
                            } else if (collectionScoped) {
                                "No relevant memory was found in the selected collections. This will retry using your full memory library."
                            } else {
                                "General knowledge will be clearly labelled and will not be presented as saved memory."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Hint
                        )
                    }
                }
            }
            item {
                Surface(
                    color = NeutralContainer,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, DividerColor)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Hint
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "No plan steps to show. ${
                                if (usingAllCollections) {
                                    "Add more relevant memories to create this plan."
                                } else if (collectionScoped) {
                                    "Search all memories to look beyond the selected collections."
                                } else {
                                    "Add a relevant memory or choose general suggestions."
                                }
                            }",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Hint
                        )
                    }
                }
            }
        }
        if (generatedSteps != null) {
            itemsIndexed(generatedSteps) { index, step -> PlanStepCard(index, step, state.completedSteps.contains(index), onToggleStep, grounding) }
        } else {
            itemsIndexed(savedSteps.orEmpty()) { index, step ->
                SavedPlanStepCard(
                    index,
                    step,
                    state.completedSteps.contains(index),
                    onToggleStep,
                    saved?.status != "COMPLETED" && !state.updatingPlanProgress
                )
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun PlanStepCard(index: Int, step: PlanStepResponse, completed: Boolean, onToggle: (Int) -> Unit, sources: List<GroundingMemoryResponse>) {
    PlanStepSurface(index, step.step, step.durationMinutes, step.reason, step.memoryIds, completed, onToggle, sources, step.sourceType, false)
}

@Composable
private fun SavedPlanStepCard(index: Int, step: SavedPlanStepResponse, completed: Boolean, onToggle: (Int) -> Unit, enabled: Boolean) {
    PlanStepSurface(index, step.step, step.durationMinutes, step.reason, step.memoryIds, completed, onToggle, emptyList(), if (step.memoryIds.isEmpty()) "GENERAL" else "MEMORY", enabled)
}

@Composable
private fun PlanStepSurface(index: Int, title: String, duration: Int?, reason: String?, memoryIds: List<String>, completed: Boolean, onToggle: (Int) -> Unit, sources: List<GroundingMemoryResponse>, sourceType: String, enabled: Boolean) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = completed,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = { onToggle(index) }
            ),
        color = if (completed) SuccessContainer else Surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            if (completed) SuccessColor.copy(alpha = .35f) else DividerColor
        )
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.Top) {
            Surface(
                modifier = Modifier.size(44.dp),
                color = if (completed) SuccessColor else SurfaceBright,
                contentColor = if (completed) Color.White else Ink,
                shape = CircleShape,
                border = BorderStroke(1.dp, if (completed) SuccessColor else DividerColor)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (completed) {
                        Icon(Icons.Default.Check, contentDescription = null)
                    } else {
                        Text("${index + 1}", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, textDecoration = if (completed) TextDecoration.LineThrough else null)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    duration?.let { MetaPill("$it min") }
                    MetaPill(if (sourceType == "GENERAL") "General guidance" else "${memoryIds.size} sources")
                }
                reason?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Hint, modifier = Modifier.padding(top = 8.dp)) }
                val linked = memoryIds.mapNotNull { id -> sources.find { it.id == id } }
                linked.forEach { Text(it.title, style = MaterialTheme.typography.bodySmall, color = Primary, maxLines = 1) }
            }
        }
    }
}

@Composable
private fun MetaPill(text: String) {
    Surface(color = NeutralContainer, shape = CircleShape, modifier = Modifier.padding(top = 8.dp)) {
        Text(text, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = Hint)
    }
}

@Composable
private fun GroundingCard(memory: GroundingMemoryResponse) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.clickable { context.startActivity(Intent(Intent.ACTION_VIEW, memory.sourceUrl.toUri())) },
        color = Surface,
        contentColor = Ink,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DividerColor.copy(alpha = .65f)),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (memory.thumbnailUrl != null) AsyncImage(memory.thumbnailUrl, null, Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
            else Box(Modifier.size(60.dp).background(AccentContainer, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text(memory.title.take(1), color = Primary) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(memory.platform.replace("_", " ").uppercase(), style = MaterialTheme.typography.labelSmall, color = Primary)
                Text(memory.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
            }
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open original source", tint = Hint)
        }
    }
}

@Composable
private fun SavedPlanCard(plan: SavedPlanResponse, onOpen: (SavedPlanResponse) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onOpen(plan) },
        color = Surface,
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(1.dp, DividerColor)
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(Ink, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                Text("I", color = Color.White, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${plan.status} · ${formatDate(plan.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = when (plan.status) {
                        "COMPLETED" -> SuccessColor
                        "ACTIVE" -> Primary
                        else -> Hint
                    }
                )
                Text(plan.goal, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "${plan.plan.count(SavedPlanStepResponse::completed)}/${plan.plan.size} complete · ${plan.retrievedMemoryIds.size} memories",
                    style = MaterialTheme.typography.bodySmall,
                    color = Hint
                )
            }
            Text("→", fontSize = 20.sp)
        }
    }
}

@Composable
private fun ProfilePage(
    session: UserSession,
    state: MainUiState,
    onSignOut: () -> Unit,
    onClearMemories: () -> Unit,
    onUpdatePersonalization: (String, String) -> Unit
) {
    var showClearConfirmation by remember { mutableStateOf(false) }
    var showPersonalization by remember { mutableStateOf(false) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { LargeTitle("SETTINGS", "Your account", "Your sources, memories, and plans remain connected to this workspace.") }
        item {
            Surface(color = Surface, shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, DividerColor)) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(62.dp).background(Primary, RoundedCornerShape(19.dp)), contentAlignment = Alignment.Center) {
                        Text(session.displayName.split(" ").take(2).joinToString("") { it.take(1) }.uppercase(), color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(session.displayName, style = MaterialTheme.typography.titleLarge)
                        Text(session.email, style = MaterialTheme.typography.bodySmall, color = Hint)
                    }
                }
            }
        }
        item {
            SettingsCard(
                icon = Icons.Default.Bookmark,
                title = "Your data",
                subtitle = "${state.memories.size} memories in ${state.collections.size} collections",
                copy = "Original source links remain attached to memories.",
                action = "Clear all",
                onClick = { showClearConfirmation = true }
            )
        }
        item {
            SettingsCard(
                icon = Icons.Default.Tune,
                title = "Plan personalization",
                subtitle = "${state.planStyle} · ${state.planPace}",
                copy = "Choose how detailed and structured your future plans should feel.",
                action = "Customize",
                onClick = { showPersonalization = true }
            )
        }
        item {
            Surface(color = ErrorContainer, shape = RoundedCornerShape(18.dp)) {
                TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().padding(6.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, tint = ErrorColor)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign out", color = ErrorColor)
                }
            }
        }
        item { Text("Impulse Android MVP", style = MaterialTheme.typography.bodySmall, color = Hint, modifier = Modifier.fillMaxWidth().padding(20.dp)) }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Clear all memories?") },
            text = {
                Text("This removes every memory and collection source. Your collection names and saved plans remain.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmation = false
                        onClearMemories()
                    },
                    enabled = state.memories.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorColor)
                ) { Text("Clear all") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) { Text("Cancel") }
            },
            containerColor = Surface
        )
    }

    if (showPersonalization) {
        PersonalizationDialog(
            currentStyle = state.planStyle,
            currentPace = state.planPace,
            onDismiss = { showPersonalization = false },
            onSave = { style, pace ->
                onUpdatePersonalization(style, pace)
                showPersonalization = false
            }
        )
    }
}

@Composable
private fun SettingsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    copy: String,
    action: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = Surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, DividerColor)
    ) {
        Row(Modifier.padding(17.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(42.dp).background(AccentContainer, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Primary) }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SuccessColor)
                Text(copy, style = MaterialTheme.typography.bodySmall, color = Hint, modifier = Modifier.padding(top = 5.dp))
            }
            Text(action, style = MaterialTheme.typography.labelLarge, color = Primary)
        }
    }
}

@Composable
private fun PersonalizationDialog(
    currentStyle: String,
    currentPace: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var style by remember { mutableStateOf(currentStyle) }
    var pace by remember { mutableStateOf(currentPace) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Personalize your plans") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("DETAIL", style = MaterialTheme.typography.labelSmall, color = Primary)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("Concise", "Balanced", "Detailed").forEach { option ->
                        FilterPill(option, style == option) { style = option }
                    }
                }
                Text("PACE", style = MaterialTheme.typography.labelSmall, color = Primary)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("Focused", "Flexible", "Relaxed").forEach { option ->
                        FilterPill(option, pace == option) { pace = option }
                    }
                }
                Text(
                    "These preferences are sent as constraints when Impulse creates future plans.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Hint
                )
            }
        },
        confirmButton = { Button(onClick = { onSave(style, pace) }) { Text("Save preferences") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = Surface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaveLinkSheet(
    collections: List<UserCollectionResponse>,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String, String?, String?) -> Unit,
    onCreateAndSave: (String, String?, String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var creatingNew by remember { mutableStateOf(false) }
    var newCollectionName by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val valid = url.startsWith("http://") || url.startsWith("https://")
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Surface, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
        Column(Modifier.fillMaxWidth().imePadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp)) {
            EditorialLabel("ADD KNOWLEDGE")
            Text("Save a useful link", style = MaterialTheme.typography.headlineMedium)
            Text("Impulse will read it, create memory, and keep the original source attached.", style = MaterialTheme.typography.bodySmall, color = Hint)
            OutlinedTextField(
                url,
                { url = it.trim() },
                label = { Text("URL") },
                placeholder = { Text("https://…") },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                shape = RoundedCornerShape(15.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = fieldColors()
            )
            OutlinedTextField(
                note,
                { note = it.take(2000) },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                minLines = 2,
                shape = RoundedCornerShape(15.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                colors = fieldColors()
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SAVE TO", style = MaterialTheme.typography.labelSmall, color = Primary, modifier = Modifier.weight(1f))
                NewCollectionAction(creatingNew) { creatingNew = !creatingNew }
            }
            if (creatingNew) {
                OutlinedTextField(
                    value = newCollectionName,
                    onValueChange = { newCollectionName = it.take(200) },
                    label = { Text("Collection name") },
                    placeholder = { Text("Weekend ideas") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                    shape = RoundedCornerShape(15.dp),
                    colors = fieldColors()
                )
            }
            CollectionChoice("ALL", "Default · Everything you save", !creatingNew && selectedId == null) {
                creatingNew = false
                selectedId = null
            }
            collections.filterNot { it.name.equals("ALL", true) }.forEach { collection ->
                CollectionChoice(collection.name, "${collection.processedSources} memories", !creatingNew && selectedId == collection.id) {
                    creatingNew = false
                    selectedId = collection.id
                }
            }
            error?.let { Text(it, color = ErrorColor, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 10.dp)) }
            Button(
                onClick = {
                    keyboard?.hide()
                    if (creatingNew) {
                        onCreateAndSave(
                            url,
                            note.trim().ifBlank { null },
                            newCollectionName.trim()
                        )
                    } else {
                        onSave(url, note.trim().ifBlank { null }, selectedId)
                    }
                },
                enabled = valid && !saving &&
                    (!creatingNew || newCollectionName.trim().length >= 2),
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp).height(54.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                if (saving) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else {
                    Text(
                        when {
                            creatingNew -> "Create collection & save"
                            selectedId == null -> "Save to ALL"
                            else -> "Save to collection"
                        }
                    )
                    Spacer(Modifier.weight(1f))
                    Text("→", fontSize = 20.sp)
                }
            }
            Text("Saving the same link again to this collection will not create a duplicate.", style = MaterialTheme.typography.bodySmall, color = Hint, modifier = Modifier.padding(top = 10.dp, bottom = 18.dp))
        }
    }
}

@Composable
private fun CollectionChoice(name: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
        color = if (selected) Secondary.copy(alpha = .48f) else SurfaceBright,
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, if (selected) Ink else DividerColor)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Folder, null, tint = if (selected) Ink else Primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(name, style = MaterialTheme.typography.titleMedium); Text(description, style = MaterialTheme.typography.bodySmall, color = Hint) }
            if (selected) Icon(Icons.Default.Check, "Selected", tint = Ink)
        }
    }
}

@Composable
private fun CollectionDialog(
    collection: UserCollectionResponse,
    onDismiss: () -> Unit,
    onUpdate: (String, String?) -> Unit,
    onDelete: () -> Unit,
    onRemoveSource: (String) -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var name by remember(collection.id) { mutableStateOf(collection.name) }
    var description by remember(collection.id) { mutableStateOf(collection.description.orEmpty()) }
    val protected = collection.name.equals("ALL", true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (editing) "Edit collection" else collection.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (!protected) IconButton(onClick = { editing = !editing }) { Icon(Icons.Default.Edit, "Edit collection") }
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (editing) {
                    OutlinedTextField(name, { name = it.take(200) }, label = { Text("Name") }, colors = fieldColors())
                    OutlinedTextField(description, { description = it.take(2000) }, label = { Text("Description") }, modifier = Modifier.padding(top = 8.dp), colors = fieldColors())
                } else {
                    Text("${collection.processedSources} ready · ${collection.failedSources} need attention", color = Hint, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    collection.sources.forEach { source -> SourceRow(source, onRemoveSource) }
                }
            }
        },
        confirmButton = {
            if (editing) Button(onClick = { onUpdate(name.trim(), description.trim().ifBlank { null }) }, enabled = name.isNotBlank()) { Text("Save changes") }
            else TextButton(onClick = onDismiss) { Text("Done") }
        },
        dismissButton = {
            if (!protected && !editing) TextButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, null, tint = ErrorColor); Text("Delete", color = ErrorColor) }
            else if (editing) TextButton(onClick = { editing = false }) { Text("Cancel") }
        },
        containerColor = Surface
    )
}

@Composable
private fun SourceRow(source: CollectionSourceResponse, onRemove: (String) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            StatusPill(if (source.status == "PROCESSED") "Ready" else source.status.replace("_", " "), source.status == "PROCESSED")
            Spacer(Modifier.width(8.dp))
            Text(source.url, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            IconButton(onClick = { onRemove(source.id) }) { Icon(Icons.Default.Close, "Remove source", tint = ErrorColor) }
        }
        HorizontalDivider(color = DividerColor)
    }
}

@Composable
private fun StatusPill(label: String, success: Boolean) {
    Surface(color = if (success) SuccessContainer else ErrorContainer, shape = CircleShape) {
        Text(label.uppercase(), Modifier.padding(horizontal = 8.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, color = if (success) SuccessColor else ErrorColor)
    }
}

@Composable
private fun FeedbackBanner(message: String, error: Boolean, modifier: Modifier, onDismiss: () -> Unit) {
    Surface(modifier.fillMaxWidth().padding(12.dp), color = if (error) ErrorContainer else SuccessContainer, shape = RoundedCornerShape(15.dp), shadowElevation = 6.dp) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = if (error) ErrorColor else SuccessColor)
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Dismiss", tint = if (error) ErrorColor else SuccessColor) }
        }
    }
}

@Composable
private fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, copy: String, action: String?, onClick: (() -> Unit)?) {
    Surface(color = Surface.copy(alpha = .65f), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, DividerColor)) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(52.dp).background(NeutralContainer, CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Primary) }
            Spacer(Modifier.height(12.dp)); Text(title, style = MaterialTheme.typography.titleLarge); Text(copy, style = MaterialTheme.typography.bodySmall, color = Hint)
            if (action != null && onClick != null) TextButton(onClick = onClick) { Text(action, color = Primary) }
        }
    }
}

@Composable
private fun LargeTitle(eyebrow: String, title: String, copy: String) {
    Column {
        EditorialLabel(eyebrow)
        Text(title, style = MaterialTheme.typography.displaySmall)
        Text(copy, style = MaterialTheme.typography.bodyMedium, color = Hint)
    }
}

@Composable
private fun SectionTitle(eyebrow: String, title: String, action: String?, onAction: (() -> Unit)?) {
    Row(verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) { EditorialLabel(eyebrow); Text(title, style = MaterialTheme.typography.titleLarge) }
        if (action != null && onAction != null) TextButton(onClick = onAction) { Text(action, color = Primary) }
    }
}

@Composable
private fun EditorialLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = Primary)
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = SurfaceBright,
    unfocusedContainerColor = SurfaceBright,
    focusedBorderColor = Primary,
    unfocusedBorderColor = DividerColor
)

private fun formatDate(value: String): String {
    return runCatching {
        val source = java.time.Instant.parse(value)
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(java.util.Date.from(source))
    }.getOrDefault(value.take(10))
}

private fun safeFileName(value: String): String =
    value.lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .take(48)
        .ifBlank { "impulse-plan" }
