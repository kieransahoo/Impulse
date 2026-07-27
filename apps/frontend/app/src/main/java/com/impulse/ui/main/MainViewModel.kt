package com.impulse.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.impulse.data.model.MemoryResponse
import com.impulse.data.model.PlanResponse
import com.impulse.data.model.SavedPlanResponse
import com.impulse.data.model.SavedPlanStepResponse
import com.impulse.data.model.UserCollectionResponse
import com.impulse.data.repository.ImpulseRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class MainDestination { HOME, MEMORIES, PLANS, PROFILE }

data class MainUiState(
    val destination: MainDestination = MainDestination.HOME,
    val navigationHistory: List<MainDestination> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val savingSource: Boolean = false,
    val searching: Boolean = false,
    val planning: Boolean = false,
    val savingPlan: Boolean = false,
    val updatingPlanProgress: Boolean = false,
    val collections: List<UserCollectionResponse> = emptyList(),
    val memories: List<MemoryResponse> = emptyList(),
    val plans: List<SavedPlanResponse> = emptyList(),
    val searchResults: List<MemoryResponse>? = null,
    val activePlan: PlanResponse? = null,
    val activeSavedPlan: SavedPlanResponse? = null,
    val completedSteps: Set<Int> = emptySet(),
    val planStyle: String = "Balanced",
    val planPace: String = "Flexible",
    val selectedPlanCollectionIds: Set<String> = emptySet(),
    val lastPlanQuery: String = "",
    val message: String? = null,
    val error: String? = null
)

class MainViewModel : ViewModel() {
    private val repository = ImpulseRepository()
    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()
    private var userId: String? = null

    fun start(userId: String) {
        if (this.userId == userId && !_state.value.loading) return
        this.userId = userId
        refresh(initial = true)
    }

    fun select(destination: MainDestination) {
        if (destination == _state.value.destination) return
        _state.value = _state.value.copy(
            destination = destination,
            navigationHistory = _state.value.navigationHistory + _state.value.destination,
            message = null,
            error = null
        )
    }

    fun back() {
        if (_state.value.activePlan != null || _state.value.activeSavedPlan != null) {
            closePlan()
            return
        }
        val history = _state.value.navigationHistory
        if (history.isEmpty()) return
        _state.value = _state.value.copy(
            destination = history.last(),
            navigationHistory = history.dropLast(1),
            message = null,
            error = null
        )
    }

    fun refresh(initial: Boolean = false) {
        val id = userId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(
                loading = initial,
                refreshing = !initial,
                error = null
            )
            val workspaceRequest = async { repository.loadWorkspace(id) }
            val plansRequest = async { repository.plans(id) }
            val workspace = workspaceRequest.await()
            val savedPlans = plansRequest.await()

            val errors = buildList {
                workspace.exceptionOrNull()?.localizedMessage?.let(::add)
                savedPlans.exceptionOrNull()?.localizedMessage?.let(::add)
            }
            _state.value = _state.value.copy(
                loading = false,
                refreshing = false,
                collections = workspace.getOrNull()?.first ?: _state.value.collections,
                memories = workspace.getOrNull()?.second ?: _state.value.memories,
                plans = savedPlans.getOrNull() ?: _state.value.plans,
                error = errors.firstOrNull()
            )
        }
    }

    fun saveSource(url: String, note: String?, collectionId: String?, onSaved: () -> Unit) {
        val id = userId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(savingSource = true, error = null, message = null)
            repository.addSource(id, collectionId, url, note).fold(
                onSuccess = {
                    _state.value = _state.value.copy(
                        savingSource = false,
                        message = "Saved to ${it.name}."
                    )
                    onSaved()
                    refresh()
                },
                onFailure = {
                    _state.value = _state.value.copy(
                        savingSource = false,
                        error = it.localizedMessage ?: "Could not save this source."
                    )
                }
            )
        }
    }

    fun saveSourceToNewCollection(
        url: String,
        note: String?,
        collectionName: String,
        onSaved: () -> Unit
    ) {
        val id = userId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(savingSource = true, error = null, message = null)
            repository.createCollectionWithSource(id, collectionName.trim(), url, note).fold(
                onSuccess = {
                    _state.value = _state.value.copy(
                        savingSource = false,
                        message = "Created ${it.name} and saved the link."
                    )
                    onSaved()
                    refresh()
                },
                onFailure = {
                    _state.value = _state.value.copy(
                        savingSource = false,
                        error = it.localizedMessage ?: "Could not create this collection."
                    )
                }
            )
        }
    }

    fun search(query: String) {
        val id = userId ?: return
        if (query.isBlank()) {
            _state.value = _state.value.copy(searchResults = null)
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(searching = true, error = null)
            repository.search(id, query.trim()).fold(
                onSuccess = {
                    _state.value = _state.value.copy(searching = false, searchResults = it)
                },
                onFailure = {
                    _state.value = _state.value.copy(
                        searching = false,
                        error = it.localizedMessage ?: "Could not search your memories."
                    )
                }
            )
        }
    }

    fun clearSearch() {
        _state.value = _state.value.copy(searchResults = null)
    }

    fun createPlan(query: String, allowGeneralKnowledge: Boolean = false) {
        val id = userId ?: return
        if (query.isBlank()) return
        viewModelScope.launch {
            val current = _state.value
            _state.value = current.copy(
                destination = MainDestination.PLANS,
                navigationHistory = if (current.destination == MainDestination.PLANS) {
                    current.navigationHistory
                } else {
                    current.navigationHistory + current.destination
                },
                planning = true,
                activePlan = null,
                activeSavedPlan = null,
                completedSteps = emptySet(),
                lastPlanQuery = query.trim(),
                error = null
            )
            repository.plan(
                id,
                query.trim(),
                buildMap {
                    put("style", _state.value.planStyle)
                    put("pace", _state.value.planPace)
                    if (_state.value.selectedPlanCollectionIds.isNotEmpty()) {
                        put("collectionIds", _state.value.selectedPlanCollectionIds.toList())
                    }
                },
                allowGeneralKnowledge
            ).fold(
                onSuccess = {
                    _state.value = _state.value.copy(planning = false, activePlan = it)
                },
                onFailure = {
                    _state.value = _state.value.copy(
                        planning = false,
                        error = it.localizedMessage ?: "Could not create your plan."
                    )
                }
            )
        }
    }

    fun togglePlanCollection(collectionId: String) {
        val selected = _state.value.selectedPlanCollectionIds.toMutableSet()
        if (!selected.add(collectionId)) selected.remove(collectionId)
        _state.value = _state.value.copy(selectedPlanCollectionIds = selected)
    }

    fun clearPlanCollections() {
        _state.value = _state.value.copy(selectedPlanCollectionIds = emptySet())
    }

    fun createStarterPlan() {
        val query = _state.value.lastPlanQuery
        if (query.isNotBlank()) createPlan(query, allowGeneralKnowledge = true)
    }

    fun retryPlanWithAllMemories() {
        val query = _state.value.lastPlanQuery
        if (query.isBlank()) return
        _state.value = _state.value.copy(selectedPlanCollectionIds = emptySet())
        createPlan(query)
    }

    fun regenerateSavedPlan(editedGoal: String) {
        val id = userId ?: return
        val saved = _state.value.activeSavedPlan ?: return
        val goal = editedGoal.trim()
        if (goal.isBlank()) return
        if (_state.value.planning) return
        viewModelScope.launch {
            _state.value = _state.value.copy(
                planning = true,
                completedSteps = emptySet(),
                lastPlanQuery = goal,
                error = null
            )
            repository.regeneratePlan(
                userId = id,
                planId = saved.id,
                constraints = buildMap {
                    put("style", _state.value.planStyle)
                    put("pace", _state.value.planPace)
                    if (_state.value.selectedPlanCollectionIds.isNotEmpty()) {
                        put("collectionIds", _state.value.selectedPlanCollectionIds.toList())
                    }
                },
                query = goal
            ).fold(
                onSuccess = {
                    _state.value = _state.value.copy(
                        planning = false,
                        activePlan = null,
                        activeSavedPlan = it,
                        plans = _state.value.plans.map { existing ->
                            if (existing.id == it.id) it else existing
                        },
                        completedSteps = emptySet(),
                        message = "Plan updated with your latest memories."
                    )
                },
                onFailure = {
                    _state.value = _state.value.copy(
                        planning = false,
                        error = it.localizedMessage ?: "Could not regenerate this plan."
                    )
                }
            )
        }
    }

    fun saveActivePlan() {
        val id = userId ?: return
        val plan = _state.value.activePlan ?: return
        if (_state.value.savingPlan) return
        viewModelScope.launch {
            _state.value = _state.value.copy(savingPlan = true, error = null)
            repository.savePlan(id, plan).fold(
                onSuccess = { saved ->
                    _state.value = _state.value.copy(
                        savingPlan = false,
                        plans = listOf(saved) + _state.value.plans.filterNot { it.id == saved.id },
                        message = "Plan saved."
                    )
                },
                onFailure = {
                    _state.value = _state.value.copy(
                        savingPlan = false,
                        error = it.localizedMessage ?: "Could not save this plan."
                    )
                }
            )
        }
    }

    fun openPlan(plan: SavedPlanResponse) {
        val current = _state.value
        _state.value = current.copy(
            destination = MainDestination.PLANS,
            navigationHistory = if (current.destination == MainDestination.PLANS) {
                current.navigationHistory
            } else {
                current.navigationHistory + current.destination
            },
            activePlan = null,
            activeSavedPlan = plan,
            completedSteps = plan.plan
                .filter(SavedPlanStepResponse::completed)
                .mapTo(mutableSetOf(), SavedPlanStepResponse::order),
            error = null
        )
    }

    fun closePlan() {
        _state.value = _state.value.copy(activePlan = null, activeSavedPlan = null, completedSteps = emptySet())
    }

    fun toggleStep(index: Int) {
        val id = userId
        val saved = _state.value.activeSavedPlan
        val step = saved?.plan?.getOrNull(index)
        if (id != null && saved != null && step != null) {
            if (_state.value.updatingPlanProgress) return
            viewModelScope.launch {
                _state.value = _state.value.copy(updatingPlanProgress = true, error = null)
                val activePlan = if (saved.status == "ACTIVE") {
                    saved
                } else {
                    repository.activatePlan(id, saved.id).getOrElse {
                        _state.value = _state.value.copy(
                            updatingPlanProgress = false,
                            error = it.localizedMessage ?: "Could not start this plan."
                        )
                        return@launch
                    }
                }
                val activeStep = activePlan.plan.getOrNull(index)
                if (activeStep == null) {
                    _state.value = _state.value.copy(
                        updatingPlanProgress = false,
                        error = "This plan step is no longer available."
                    )
                    return@launch
                }
                repository.updatePlanStep(
                    id,
                    activePlan.id,
                    activeStep.id,
                    !activeStep.completed
                ).fold(
                    onSuccess = { applySavedPlanProgress(it) },
                    onFailure = {
                        _state.value = _state.value.copy(
                            updatingPlanProgress = false,
                            error = it.localizedMessage ?: "Could not update this step."
                        )
                    }
                )
            }
            return
        }
        val complete = _state.value.completedSteps.toMutableSet()
        if (!complete.add(index)) complete.remove(index)
        _state.value = _state.value.copy(completedSteps = complete)
    }

    fun activateSavedPlan() {
        val id = userId ?: return
        val saved = _state.value.activeSavedPlan ?: return
        if (_state.value.updatingPlanProgress) return
        viewModelScope.launch {
            _state.value = _state.value.copy(updatingPlanProgress = true, error = null)
            repository.activatePlan(id, saved.id).fold(
                onSuccess = {
                    applySavedPlanProgress(it)
                    _state.value = _state.value.copy(message = "Plan activated.")
                },
                onFailure = {
                    _state.value = _state.value.copy(
                        updatingPlanProgress = false,
                        error = it.localizedMessage ?: "Could not activate this plan."
                    )
                }
            )
        }
    }

    fun completeSavedPlan() {
        val id = userId ?: return
        val saved = _state.value.activeSavedPlan ?: return
        if (_state.value.updatingPlanProgress) return
        viewModelScope.launch {
            _state.value = _state.value.copy(updatingPlanProgress = true, error = null)
            repository.completePlan(id, saved.id).fold(
                onSuccess = {
                    applySavedPlanProgress(it)
                    _state.value = _state.value.copy(message = "Plan completed.")
                },
                onFailure = {
                    _state.value = _state.value.copy(
                        updatingPlanProgress = false,
                        error = it.localizedMessage ?: "Complete every task first."
                    )
                }
            )
        }
    }

    fun deleteSavedPlan() {
        val id = userId ?: return
        val saved = _state.value.activeSavedPlan ?: return
        if (_state.value.updatingPlanProgress) return
        viewModelScope.launch {
            _state.value = _state.value.copy(updatingPlanProgress = true, error = null)
            repository.deletePlan(id, saved.id).fold(
                onSuccess = {
                    _state.value = _state.value.copy(
                        updatingPlanProgress = false,
                        activeSavedPlan = null,
                        completedSteps = emptySet(),
                        plans = _state.value.plans.filterNot { it.id == saved.id },
                        message = "Plan deleted."
                    )
                },
                onFailure = {
                    _state.value = _state.value.copy(
                        updatingPlanProgress = false,
                        error = it.localizedMessage ?: "Could not delete this plan."
                    )
                }
            )
        }
    }

    private fun applySavedPlanProgress(plan: SavedPlanResponse) {
        _state.value = _state.value.copy(
            updatingPlanProgress = false,
            activeSavedPlan = plan,
            plans = _state.value.plans.map { if (it.id == plan.id) plan else it },
            completedSteps = plan.plan
                .filter(SavedPlanStepResponse::completed)
                .mapTo(mutableSetOf(), SavedPlanStepResponse::order)
        )
    }

    fun updateCollection(collectionId: String, name: String, description: String?) {
        val id = userId ?: return
        viewModelScope.launch {
            repository.updateCollection(id, collectionId, name, description).fold(
                onSuccess = { refresh() },
                onFailure = {
                    _state.value = _state.value.copy(error = it.localizedMessage ?: "Could not update this collection.")
                }
            )
        }
    }

    fun deleteCollection(collectionId: String) {
        val id = userId ?: return
        viewModelScope.launch {
            repository.deleteCollection(id, collectionId).fold(
                onSuccess = { refresh() },
                onFailure = {
                    _state.value = _state.value.copy(error = it.localizedMessage ?: "Could not delete this collection.")
                }
            )
        }
    }

    fun removeSource(collectionId: String, sourceId: String) {
        val id = userId ?: return
        viewModelScope.launch {
            repository.removeSource(id, collectionId, sourceId).fold(
                onSuccess = { refresh() },
                onFailure = {
                    _state.value = _state.value.copy(error = it.localizedMessage ?: "Could not remove this source.")
                }
            )
        }
    }

    fun changeMemoryCollection(memory: MemoryResponse, collectionId: String?) {
        val id = userId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(error = null, message = null)
            repository.addSource(id, collectionId, memory.sourceUrl, null).fold(
                onSuccess = { destination ->
                    removePreviousCollectionSources(id, memory.id, destination.id)
                    _state.value = _state.value.copy(message = "Moved to ${destination.name}.")
                    refresh()
                },
                onFailure = {
                    _state.value = _state.value.copy(
                        error = it.localizedMessage ?: "Could not change this memory's collection."
                    )
                }
            )
        }
    }

    fun createCollectionForMemory(memory: MemoryResponse, name: String) {
        val id = userId ?: return
        val trimmed = name.trim()
        if (trimmed.length < 2) {
            _state.value = _state.value.copy(error = "Collection name must contain at least 2 characters.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(error = null, message = null)
            repository.createCollection(id, trimmed, null, listOf(memory.sourceUrl)).fold(
                onSuccess = { destination ->
                    removePreviousCollectionSources(id, memory.id, destination.id)
                    _state.value = _state.value.copy(message = "Created ${destination.name} and moved the memory.")
                    refresh()
                },
                onFailure = {
                    _state.value = _state.value.copy(
                        error = it.localizedMessage ?: "Could not create this collection."
                    )
                }
            )
        }
    }

    private suspend fun removePreviousCollectionSources(
        userId: String,
        memoryId: String,
        destinationCollectionId: String
    ) {
        _state.value.collections.flatMap { collection ->
            collection.sources
                .filter { it.memoryId == memoryId && collection.id != destinationCollectionId }
                .map { collection.id to it.id }
        }.forEach { (oldCollectionId, sourceId) ->
            repository.removeSource(userId, oldCollectionId, sourceId)
        }
    }

    fun clearFeedback() {
        _state.value = _state.value.copy(message = null, error = null)
    }

    fun deleteMemory(memoryId: String) {
        val id = userId ?: return
        viewModelScope.launch {
            repository.deleteMemory(id, memoryId).fold(
                onSuccess = {
                    _state.value = _state.value.copy(message = "Memory removed.")
                    refresh()
                },
                onFailure = {
                    _state.value = _state.value.copy(error = it.localizedMessage ?: "Could not remove this memory.")
                }
            )
        }
    }

    fun clearAllMemories() {
        val id = userId ?: return
        viewModelScope.launch {
            repository.clearMemories(id).fold(
                onSuccess = {
                    _state.value = _state.value.copy(
                        memories = emptyList(),
                        searchResults = null,
                        message = "All memories cleared."
                    )
                    refresh()
                },
                onFailure = {
                    _state.value = _state.value.copy(error = it.localizedMessage ?: "Could not clear your memories.")
                }
            )
        }
    }

    fun updatePersonalization(style: String, pace: String) {
        _state.value = _state.value.copy(
            planStyle = style,
            planPace = pace,
            message = "Plan personalization updated."
        )
    }
}
