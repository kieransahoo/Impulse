package com.impulse.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.impulse.data.model.MemoryResponse
import com.impulse.data.model.UserCollectionResponse
import com.impulse.data.repository.ImpulseRepository
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SharedUrlItem(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val title: String,
    val summary: String = "",
    val platform: String = "WEB",
    val thumbnailUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class HomeUiState(
    val loading: Boolean = false,
    val creating: Boolean = false,
    val collections: List<UserCollectionResponse> = emptyList(),
    val memories: List<SharedUrlItem> = emptyList(),
    val message: String? = null,
    val error: String? = null
)

class HomeViewModel : ViewModel() {
    private val repository = ImpulseRepository()
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    val sharedUrls: StateFlow<List<SharedUrlItem>> = MutableStateFlow(emptyList())

    fun loadRecentUrls(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            repository.loadWorkspace(userId).fold(
                onSuccess = { (collections, memories) ->
                    _uiState.value = HomeUiState(
                        collections = collections,
                        memories = memories.map(MemoryResponse::toUiItem)
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = it.localizedMessage ?: "Could not load your memory space."
                    )
                }
            )
        }
    }

    fun createCollection(
        userId: String,
        name: String,
        description: String?,
        urls: List<String>
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(creating = true, error = null, message = null)
            repository.createCollection(userId, name, description, urls).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        creating = false,
                        message = "${it.processedSources} memories created; ${it.failedSources} need attention."
                    )
                    loadRecentUrls(userId)
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        creating = false,
                        error = it.localizedMessage ?: "Could not create this collection."
                    )
                }
            )
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }

    fun updateCollection(
        userId: String,
        collectionId: String,
        name: String,
        description: String?
    ) {
        viewModelScope.launch {
            repository.updateCollection(userId, collectionId, name, description).fold(
                onSuccess = { loadRecentUrls(userId) },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        error = it.localizedMessage ?: "Could not update this collection."
                    )
                }
            )
        }
    }

    fun deleteCollection(userId: String, collectionId: String) {
        viewModelScope.launch {
            repository.deleteCollection(userId, collectionId).fold(
                onSuccess = { loadRecentUrls(userId) },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        error = it.localizedMessage ?: "Could not delete this collection."
                    )
                }
            )
        }
    }

    fun removeSource(userId: String, collectionId: String, sourceId: String) {
        viewModelScope.launch {
            repository.removeSource(userId, collectionId, sourceId).fold(
                onSuccess = { loadRecentUrls(userId) },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        error = it.localizedMessage ?: "Could not remove this source."
                    )
                }
            )
        }
    }
}

private fun MemoryResponse.toUiItem() = SharedUrlItem(
    id = id,
    url = sourceUrl,
    title = title,
    summary = summary,
    platform = platform ?: category,
    thumbnailUrl = thumbnailUrl
)
