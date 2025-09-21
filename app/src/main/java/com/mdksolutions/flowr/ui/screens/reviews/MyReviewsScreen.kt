package com.mdksolutions.flowr.ui.screens.reviews

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mdksolutions.flowr.model.Product
import com.mdksolutions.flowr.viewmodel.MyReviewsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReviewsScreen(
    navController: NavController,
    vm: MyReviewsViewModel = viewModel()
) {
    // ✅ Collect the StateFlow (this fixes the "Property delegate must have getValue" error)
    val ui by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("My Reviews") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                ui.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                ui.error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(ui.error ?: "Error", color = MaterialTheme.colorScheme.error)
                    }
                }
                ui.products.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("You haven't reviewed any products yet.")
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = ui.products,
                            key = { it.id },
                            contentType = { "product_row" }
                        ) { p ->
                            ReviewedProductRow(
                                product = p,
                                onClick = { navController.navigate("product_detail/${p.id}") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewedProductRow(product: Product, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(product.name, style = MaterialTheme.typography.titleMedium)
            if (product.brand.isNotBlank()) {
                Text("Brand: ${product.brand}", style = MaterialTheme.typography.bodyMedium)
            }
            if (product.category.isNotBlank()) {
                Text("Category: ${product.category}", style = MaterialTheme.typography.bodyMedium)
            }
            if (product.strainType.isNotBlank()) {
                Text("Strain: ${product.strainType}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
