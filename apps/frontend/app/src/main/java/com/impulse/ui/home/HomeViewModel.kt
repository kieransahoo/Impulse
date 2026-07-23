package com.impulse.ui.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/** A URL that was previously shared through the app. */
data class SharedUrlItem(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)

class HomeViewModel : ViewModel() {

    private val _sharedUrls = MutableStateFlow<List<SharedUrlItem>>(emptyList())
    val sharedUrls: StateFlow<List<SharedUrlItem>> = _sharedUrls.asStateFlow()

    /**
     * In a real app these would be fetched from the backend.
     * For the MVP we pre-populate with representative mock data.
     */
    fun loadRecentUrls(userId: String) {
        _sharedUrls.value = listOf(
            SharedUrlItem(
                url   = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                title = "YouTube – Never Gonna Give You Up"
            ),
            SharedUrlItem(
                url   = "https://www.instagram.com/p/C9xYZ_EXAMPLE/",
                title = "Instagram Post"
            ),
            SharedUrlItem(
                url   = "https://youtu.be/jNQXAC9IVRw",
                title = "YouTube Short"
            ),
            SharedUrlItem(
                url   = "https://www.instagram.com/reel/EXAMPLE_REEL/",
                title = "Instagram Reel"
            )
        )
    }
}
