package com.mdksolutions.flowr.ui.screens.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.mdksolutions.flowr.model.Product
import com.mdksolutions.flowr.viewmodel.PublicProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(
    navController: NavController,
    vm: PublicProfileViewModel = viewModel()
) {
    val ui by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                ui.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                ui.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(ui.error!!, color = MaterialTheme.colorScheme.error) }
                else -> {
                    ui.profile?.let { p ->
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    val painter = rememberAsyncImagePainter(p.photoUrl)
                                    Surface(
                                        modifier = Modifier.size(72.dp).clip(CircleShape),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        if (!p.photoUrl.isNullOrBlank()) {
                                            Image(
                                                painter = painter,
                                                contentDescription = "Avatar",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = p.displayName.ifBlank { "Unnamed" },
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        if (p.role.isNotBlank()) {
                                            Text("Role: ${p.role}", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                // Follow / Unfollow button (hidden on your own profile)
                                if (!ui.isSelf) {
                                    val following = ui.isFollowing
                                    val label = if (following) "Unfollow" else "Follow"
                                    Button(
                                        enabled = !ui.isBusy,
                                        onClick = { if (following) vm.unfollow() else vm.follow() }
                                    ) {
                                        Text(label)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                }

                                AssistChip(onClick = { /* read-only */ }, label = { Text("Reviews: ${ui.reviewCount}") })
                            }

                            if (ui.products.isEmpty()) {
                                item {
                                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text("No reviews yet.")
                                    }
                                }
                            } else {
                                items(
                                    items = ui.products,
                                    key = { it.id },
                                    contentType = { "product_row" }
                                ) { prod ->
                                    ProductRowReadonly(
                                        product = prod,
                                        onOpen = { navController.navigate("product_detail/${prod.id}") }
                                    )
                                }
                            }
                        }
                    } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Profile not found")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductRowReadonly(product: Product, onOpen: () -> Unit) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(product.name, style = MaterialTheme.typography.titleMedium)
            if (product.brand.isNotBlank()) Text("Brand: ${product.brand}", style = MaterialTheme.typography.bodyMedium)
            if (product.category.isNotBlank()) Text("Category: ${product.category}", style = MaterialTheme.typography.bodyMedium)
            if (product.strainType.isNotBlank()) Text("Strain: ${product.strainType}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
