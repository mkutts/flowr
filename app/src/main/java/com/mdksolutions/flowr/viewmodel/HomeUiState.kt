package com.mdksolutions.flowr.viewmodel

import com.mdksolutions.flowr.model.Product

data class HomeUiState(
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val selectedState: String? = null,
    val selectedFeel: String? = null,
    val selectedActivity: String? = null
)
