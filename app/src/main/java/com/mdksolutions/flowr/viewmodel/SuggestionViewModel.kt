package com.mdksolutions.flowr.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.mdksolutions.flowr.model.Suggestion
import com.mdksolutions.flowr.repository.SuggestionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SuggestionUiState(
    val type: String = "Feature",
    val title: String = "",
    val description: String = "",
    val severity: String = "Low",
    val stepsToReproduce: String = "",
    val contactEmail: String = "",
    val screenshotUrl: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val successId: String? = null
)

class SuggestionViewModel(
    private val repo: SuggestionRepository = SuggestionRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _ui = MutableStateFlow(SuggestionUiState())
    val ui: StateFlow<SuggestionUiState> = _ui

    fun update(transform: (SuggestionUiState) -> SuggestionUiState) {
        _ui.value = transform(_ui.value)
    }

    fun submit() {
        val state = _ui.value

        // validation
        if (state.title.isBlank()) { _ui.value = state.copy(error = "Title is required"); return }
        if (state.description.isBlank()) { _ui.value = state.copy(error = "Description is required"); return }

        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            _ui.value = state.copy(error = "Please sign in to send feedback.")
            return
        }

        viewModelScope.launch {
            try {
                _ui.value = state.copy(isSubmitting = true, error = null, successId = null)

                val suggestion = Suggestion(
                    userId = uid,                    // ← ensure it matches request.auth.uid
                    type = state.type,
                    title = state.title.trim(),
                    description = state.description.trim(),
                    severity = if (state.type == "Bug") state.severity else null,
                    stepsToReproduce = if (state.type == "Bug") state.stepsToReproduce.trim().ifBlank { null } else null,
                    contactEmail = state.contactEmail.trim().ifBlank { null },
                    screenshotUrl = state.screenshotUrl.trim().ifBlank { null }
                    // createdAt/status come from defaults in the data class
                )

                val id = repo.create(suggestion)
                _ui.value = _ui.value.copy(isSubmitting = false, successId = id)
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(isSubmitting = false, error = t.message ?: "Failed to submit")
            }
        }
    }

    fun clearAlerts() {
        _ui.value = _ui.value.copy(error = null, successId = null)
    }
}
