package com.mdksolutions.flowr.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mdksolutions.flowr.model.BudtenderWork
import com.mdksolutions.flowr.model.WEEK_DAYS
import com.mdksolutions.flowr.model.WorkShift
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BudtenderWorkUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val work: BudtenderWork = BudtenderWork()
)

class BudtenderWorkViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(BudtenderWorkUiState())
    val uiState: StateFlow<BudtenderWorkUiState> = _uiState

    // Firestore doc: /users/{uid}/work/info
    private fun workDoc(uid: String) =
        db.collection("users").document(uid)
            .collection("work").document("info")

    init { refresh() }

    fun refresh() {
        val user = auth.currentUser ?: run {
            _uiState.value = BudtenderWorkUiState(isLoading = false, error = "Not signed in")
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val snap = workDoc(user.uid).get().awaitk()
                val work = if (snap.exists())
                    snap.toObject(BudtenderWork::class.java)   // Java-style, no KTX import needed
                else
                    null

                val finalWork = work ?: BudtenderWork(
                    schedule = WEEK_DAYS.associateWith { emptyList<WorkShift>() }
                )
                _uiState.update { it.copy(isLoading = false, work = finalWork) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load work info") }
            }
        }
    }

    fun saveWork(updated: BudtenderWork) {
        val user = auth.currentUser ?: run {
            _uiState.update { it.copy(error = "Not signed in") }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val normalized = ensureAllDays(updated)
                workDoc(user.uid).set(normalized).awaitk()
                _uiState.update { it.copy(isLoading = false, work = normalized) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to save work info") }
            }
        }
    }

    fun setDispensary(name: String, address: String, placeId: String?) {
        val cur = _uiState.value.work
        saveWork(cur.copy(dispensaryName = name, address = address, placeId = placeId))
    }

    fun addShift(dayLowercase: String, start: String, end: String) {
        val day = dayLowercase.lowercase()
        val cur = _uiState.value.work
        val list = (cur.schedule[day] ?: emptyList()) + WorkShift(start, end)
        saveWork(cur.copy(schedule = cur.schedule + (day to list)))
    }

    fun removeShift(dayLowercase: String, index: Int) {
        val day = dayLowercase.lowercase()
        val cur = _uiState.value.work
        val list = cur.schedule[day]?.toMutableList() ?: return
        if (index !in list.indices) return
        list.removeAt(index)
        saveWork(cur.copy(schedule = cur.schedule + (day to list)))
    }

    private fun ensureAllDays(work: BudtenderWork): BudtenderWork {
        val withAll = work.schedule.toMutableMap()
        WEEK_DAYS.forEach { d -> if (withAll[d] == null) withAll[d] = emptyList() }
        return work.copy(schedule = withAll)
    }
}
