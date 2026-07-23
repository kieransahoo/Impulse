package com.impulse.ui.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.impulse.data.model.UserCollectionResponse
import com.impulse.data.repository.ShareRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ShareUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val collections: List<UserCollectionResponse> = emptyList(),
    val selectedCollectionId: String? = null,
    val error: String? = null,
    val saved: Boolean = false
)

class ShareViewModel : ViewModel() {
    private val repository = ShareRepository()
    private val _state = MutableStateFlow(ShareUiState())
    val state: StateFlow<ShareUiState> = _state.asStateFlow()

    fun load(userId: String) {
        viewModelScope.launch {
            repository.collections(userId).fold(
                onSuccess = { collections ->
                    _state.value = ShareUiState(
                        loading = false,
                        collections = collections.filterNot { it.name.equals("ALL", true) }
                    )
                },
                onFailure = {
                    _state.value = ShareUiState(
                        loading = false,
                        error = it.localizedMessage ?: "Could not load collections."
                    )
                }
            )
        }
    }

    fun select(collectionId: String?) {
        _state.value = _state.value.copy(selectedCollectionId = collectionId, error = null)
    }

    fun save(userId: String, url: String, note: String?) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null)
            val selectedName = _state.value.selectedCollectionId
                ?.let { id -> _state.value.collections.firstOrNull { it.id == id }?.name }
                ?: "ALL"
            repository.save(userId, url, selectedName, note).fold(
                onSuccess = { _state.value = _state.value.copy(saving = false, saved = true) },
                onFailure = {
                    _state.value = _state.value.copy(
                        saving = false,
                        error = it.localizedMessage ?: "Could not save this link."
                    )
                }
            )
        }
    }
}
