package com.mdksolutions.flowr.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.mdksolutions.flowr.data.LookupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LookupUiState(
    val allBrands: List<String> = emptyList(),
    val allStrains: List<String> = emptyList(),
    val allOther: List<String> = emptyList(),
    val brandQuery: String = "",
    val strainQuery: String = "",
    val otherQuery: String = "",
    val brandSuggestions: List<String> = emptyList(),
    val strainSuggestions: List<String> = emptyList(),
    val otherSuggestions: List<String> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

class LookupViewModel(
    private val repo: LookupRepository = LookupRepository(FirebaseFirestore.getInstance())
) : ViewModel() {

    private val _ui = MutableStateFlow(LookupUiState(loading = true))
    val ui: StateFlow<LookupUiState> = _ui

    init {
        loadLookups()
    }

    fun loadLookups() {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            try {
                val brands = repo.getLookups("brands")
                val strains = repo.getLookups("strains")
                val other = repo.getLookups("other")
                _ui.update {
                    it.copy(
                        allBrands = brands,
                        allStrains = strains,
                        allOther = other,
                        brandSuggestions = brands,
                        strainSuggestions = strains,
                        otherSuggestions = other,
                        loading = false
                    )
                }
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.message ?: "Failed to load lookups") }
            }
        }
    }

    fun onBrandQueryChange(q: String) {
        _ui.update { st ->
            st.copy(
                brandQuery = q,
                brandSuggestions = filter(st.allBrands, q)
            )
        }
    }

    fun onStrainQueryChange(q: String) {
        _ui.update { st ->
            st.copy(
                strainQuery = q,
                strainSuggestions = filter(st.allStrains, q)
            )
        }
    }

    fun onOtherQueryChange(q: String) {
        _ui.update { st ->
            st.copy(
                otherQuery = q,
                otherSuggestions = filter(st.allOther, q)
            )
        }
    }

    private fun filter(source: List<String>, query: String): List<String> {
        if (query.isBlank()) return source.take(15)
        val q = query.trim().lowercase()
        // Starts-with ranked first, then contains
        val starts = source.filter { it.lowercase().startsWith(q) }
        val contains = source.filter { it.lowercase().contains(q) && !it.lowercase().startsWith(q) }
        return (starts + contains).take(15)
    }
}
