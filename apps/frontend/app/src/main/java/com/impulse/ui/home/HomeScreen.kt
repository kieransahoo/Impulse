package com.impulse.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.impulse.ui.home.HomeViewModel
import com.impulse.ui.home.SharedUrlItem
import com.impulse.data.model.UserSession
import com.impulse.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    session: UserSession?,
    onNavigateToChat: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val sharedUrls  by viewModel.sharedUrls.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(session?.userId) {
        session?.let { viewModel.loadRecentUrls(it.userId) }
    }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            AppBottomBar(
                selectedTab  = selectedTab,
                onTabSelected = { tab ->
                    selectedTab = tab
                    if (tab == 1) onNavigateToChat()
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            item {
                HomeHeader(session = session, onSignOut = onSignOut)
            }

            // ── Stats banner ──────────────────────────────────────────────────
            item {
                StatsBanner(urlCount = sharedUrls.size)
            }

            // ── Quick-chat CTA ─────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(4.dp))
                ChatCta(onClick = onNavigateToChat)
                Spacer(Modifier.height(4.dp))
            }

            // ── Section title ─────────────────────────────────────────────────
            item {
                Text(
                    text       = "Recent Shared Links",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = OnBackground,
                    modifier   = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }

            // ── URL cards ─────────────────────────────────────────────────────
            if (sharedUrls.isEmpty()) {
                item { EmptyLinksState() }
            } else {
                items(sharedUrls, key = { it.id }) { item ->
                    SharedLinkCard(item = item)
                }
            }
        }
    }
}

// ─── Header ───────────────────────────────────────────────────────────────────

@Composable
private fun HomeHeader(session: UserSession?, onSignOut: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Hello,", fontSize = 14.sp, color = Hint)
            Text(
                text       = session?.displayName?.split(" ")?.firstOrNull() ?: "User",
                fontSize   = 26.sp,
                fontWeight = FontWeight.Bold,
                color      = OnBackground
            )
        }

        // Avatar
        if (session?.photoUrl != null) {
            AsyncImage(
                model              = session.photoUrl,
                contentDescription = "Avatar",
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.size(46.dp).clip(CircleShape)
            )
        } else {
            Box(
                modifier        = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Primary, Secondary))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = session?.displayName?.firstOrNull()?.uppercase() ?: "U",
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 20.sp
                )
            }
        }

        Spacer(Modifier.width(6.dp))

        IconButton(onClick = onSignOut) {
            Icon(Icons.Default.Logout, contentDescription = "Sign out", tint = Hint)
        }
    }
}

// ─── Stats banner ─────────────────────────────────────────────────────────────

@Composable
private fun StatsBanner(urlCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(Primary.copy(alpha = 0.85f), Secondary.copy(alpha = 0.70f))
                )
            )
            .padding(24.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text("Links Saved", fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f))
                Text(
                    text       = urlCount.toString(),
                    fontSize   = 52.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color.White
                )
            }
            Icon(
                imageVector        = Icons.Default.Link,
                contentDescription = null,
                tint               = Color.White.copy(alpha = 0.4f),
                modifier           = Modifier.size(72.dp)
            )
        }
    }
}

// ─── Chat CTA ─────────────────────────────────────────────────────────────────

@Composable
private fun ChatCta(onClick: () -> Unit) {
    OutlinedButton(
        onClick  = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(52.dp),
        shape  = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Primary.copy(alpha = 0.6f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
    ) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("Open AI Chat", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

// ─── URL card ─────────────────────────────────────────────────────────────────

@Composable
private fun SharedLinkCard(item: SharedUrlItem) {
    val fmt        = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val isYoutube  = item.url.contains("youtube") || item.url.contains("youtu.be")
    val isInsta    = item.url.contains("instagram")

    val iconEmoji  = if (isYoutube) "▶" else if (isInsta) "📸" else "🔗"
    val iconBg     = when {
        isYoutube -> Color(0xFFFF0000).copy(alpha = 0.12f)
        isInsta   -> Color(0xFFE1306C).copy(alpha = 0.12f)
        else      -> Primary.copy(alpha = 0.12f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp),
        shape  = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, DividerColor)
    ) {
        Row(
            modifier          = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier        = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Text(iconEmoji, fontSize = 22.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text       = item.title,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = OnBackground,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Text(
                    text     = item.url,
                    fontSize = 12.sp,
                    color    = Hint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text     = fmt.format(Date(item.timestamp)),
                    fontSize = 11.sp,
                    color    = Hint.copy(alpha = 0.6f)
                )
            }

            Icon(
                imageVector        = Icons.Default.ChevronRight,
                contentDescription = null,
                tint               = Hint,
                modifier           = Modifier.size(18.dp)
            )
        }
    }
}

// ─── Empty state ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyLinksState() {
    Column(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        Text("🔗", fontSize = 52.sp)
        Spacer(Modifier.height(14.dp))
        Text(
            text       = "No links yet",
            fontSize   = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color      = OnBackground,
            textAlign  = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text      = "Share a video from YouTube or\na post from Instagram and it'll\nappear here automatically.",
            fontSize  = 14.sp,
            color     = Hint,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

// ─── Bottom nav ───────────────────────────────────────────────────────────────

@Composable
private fun AppBottomBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    NavigationBar(
        containerColor = Surface,
        tonalElevation = 0.dp,
        modifier       = Modifier.navigationBarsPadding()
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick  = { onTabSelected(0) },
            icon     = { Icon(Icons.Default.Home, "Home") },
            label    = { Text("Home") },
            colors   = NavigationBarItemDefaults.colors(
                selectedIconColor   = Primary,
                selectedTextColor   = Primary,
                unselectedIconColor = Hint,
                unselectedTextColor = Hint,
                indicatorColor      = Primary.copy(alpha = 0.15f)
            )
        )
        NavigationBarItem(
            selected = selectedTab == 1,
            onClick  = { onTabSelected(1) },
            icon     = { Icon(Icons.Default.AutoAwesome, "Chat") },
            label    = { Text("AI Chat") },
            colors   = NavigationBarItemDefaults.colors(
                selectedIconColor   = Primary,
                selectedTextColor   = Primary,
                unselectedIconColor = Hint,
                unselectedTextColor = Hint,
                indicatorColor      = Primary.copy(alpha = 0.15f)
            )
        )
    }
}
