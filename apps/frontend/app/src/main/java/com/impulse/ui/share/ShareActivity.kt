package com.impulse.ui.share

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.impulse.data.local.SessionManager
import com.impulse.data.model.UserCollectionResponse
import com.impulse.ui.theme.AccentContainer
import com.impulse.ui.theme.DividerColor
import com.impulse.ui.theme.ErrorColor
import com.impulse.ui.theme.ErrorContainer
import com.impulse.ui.theme.Hint
import com.impulse.ui.theme.ImpulseTheme
import com.impulse.ui.theme.Ink
import com.impulse.ui.theme.NeutralContainer
import com.impulse.ui.theme.Paper
import com.impulse.ui.theme.Primary
import com.impulse.ui.theme.Secondary
import com.impulse.ui.theme.Surface
import com.impulse.ui.theme.SurfaceBright
import com.impulse.utils.extractUrl
import com.impulse.utils.isValidUrl

class ShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent?.getStringExtra(Intent.EXTRA_TEXT)?.extractUrl().orEmpty()
        val session = SessionManager.getInstance(this).getSession()

        setContent {
            ImpulseTheme {
                ShareCaptureScreen(
                    url = url,
                    userId = session?.userId,
                    onClose = ::finish,
                    onOpenApp = {
                        startActivity(Intent(this, com.impulse.MainActivity::class.java))
                        finish()
                    },
                    onSaved = {
                        Toast.makeText(this, "Saved to your memory", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun ShareCaptureScreen(
    url: String,
    userId: String?,
    onClose: () -> Unit,
    onOpenApp: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ShareViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var note by remember { mutableStateOf("") }

    LaunchedEffect(userId) {
        userId?.let(viewModel::load)
    }
    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
            }
            Column(Modifier.weight(1f)) {
                Text("SAVE TO IMPULSE", style = MaterialTheme.typography.labelSmall, color = Primary)
                Text("Choose a collection", style = MaterialTheme.typography.titleLarge)
            }
            ImpulseShareMark()
        }
        Spacer(Modifier.height(22.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, DividerColor)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(54.dp).background(AccentContainer, RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.BookmarkAdd, contentDescription = null, tint = Primary)
                }
                Spacer(Modifier.size(13.dp))
                Column {
                    Text("Shared link", style = MaterialTheme.typography.labelSmall, color = Hint)
                    Text(
                        url.ifBlank { "No valid link found" },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        if (userId == null) {
            EmptyAuthCard(onOpenApp)
            return@Column
        }
        if (!url.isValidUrl()) {
            ErrorCard("Impulse could not find a valid HTTP or HTTPS link in this share.")
            return@Column
        }

        Text("COLLECTION", style = MaterialTheme.typography.labelSmall, color = Primary)
        Spacer(Modifier.height(9.dp))
        CollectionChoice(
            name = "ALL",
            description = "Default · Everything you save",
            selected = state.selectedCollectionId == null,
            onClick = { viewModel.select(null) }
        )
        if (state.loading) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            state.collections.forEach { collection ->
                CollectionChoice(
                    name = collection.name,
                    description = "${collection.processedSources} memories",
                    selected = state.selectedCollectionId == collection.id,
                    onClick = { viewModel.select(collection.id) }
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        OutlinedTextField(
            value = note,
            onValueChange = { note = it.take(2_000) },
            label = { Text("Add a note (optional)") },
            placeholder = { Text("Why do you want to remember this?") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            shape = RoundedCornerShape(13.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceBright,
                unfocusedContainerColor = SurfaceBright,
                focusedBorderColor = Primary,
                unfocusedBorderColor = DividerColor
            )
        )

        state.error?.let {
            Spacer(Modifier.height(12.dp))
            ErrorCard(it)
        }
        Spacer(Modifier.height(22.dp))
        Button(
            onClick = { viewModel.save(userId, url, note.trim().ifBlank { null }) },
            enabled = !state.saving && !state.loading,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            if (state.saving) {
                CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text(
                    if (state.selectedCollectionId == null) "Save to ALL"
                    else "Save to collection",
                    modifier = Modifier.weight(1f)
                )
                Text("→", fontSize = 20.sp)
            }
        }
        Text(
            "Only this link will be saved. Sharing it again to the same collection will not create a duplicate.",
            style = MaterialTheme.typography.bodySmall,
            color = Hint,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun CollectionChoice(
    name: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable(onClick = onClick),
        color = if (selected) Secondary.copy(alpha = .55f) else Surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (selected) Ink else DividerColor)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = if (selected) Ink else Primary)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = Hint)
            }
            Box(
                modifier = Modifier.size(24.dp).background(
                    if (selected) Ink else NeutralContainer,
                    CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun EmptyAuthCard(onOpenApp: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Sign in before saving", style = MaterialTheme.typography.titleLarge)
        Text("Open Impulse and sign in, then share this link again.", color = Hint)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onOpenApp, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
            Text("Open Impulse")
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Surface(color = ErrorContainer, shape = RoundedCornerShape(12.dp)) {
        Text(message, color = ErrorColor, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth().padding(14.dp))
    }
}

@Composable
private fun ImpulseShareMark() {
    Box(
        modifier = Modifier.size(36.dp).background(Ink, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("I", color = Color.White, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)
    }
}
