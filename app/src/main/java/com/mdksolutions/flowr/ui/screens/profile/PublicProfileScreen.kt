package com.mdksolutions.flowr.ui.screens.profile

import android.content.Intent
import android.content.ActivityNotFoundException // ⬅️ NEW
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.mdksolutions.flowr.model.Product
import com.mdksolutions.flowr.model.BudtenderWork
import com.mdksolutions.flowr.model.WEEK_DAYS
import com.mdksolutions.flowr.viewmodel.PublicProfileViewModel
import java.net.URLEncoder
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(
    navController: NavController,
    vm: PublicProfileViewModel = viewModel()
) {
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
                                        // NEW: show @username under the display name
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "@" + (p.username.ifBlank { "unknown" }),
                                            style = MaterialTheme.typography.bodyMedium
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

                            // Work section (if the user has provided work info)
                            ui.work?.let { work ->
                                item {
                                    WorkSection(
                                        work = work,
                                        onOpenMaps = { openInMaps(context, work) }
                                    )
                                }
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
private fun WorkSection(work: BudtenderWork, onOpenMaps: () -> Unit) {
    ElevatedCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Work", style = MaterialTheme.typography.titleMedium)
            if (work.dispensaryName.isNotBlank()) {
                Text("Dispensary: ${work.dispensaryName}")
            }
            if (work.address.isNotBlank()) {
                Text(work.address, style = MaterialTheme.typography.bodyMedium)
            }
            OutlinedButton(onClick = onOpenMaps) { Text("Open in Google Maps") }

            HorizontalDivider(
                Modifier.padding(vertical = 8.dp),
                DividerDefaults.Thickness,
                DividerDefaults.color
            )

            val hasAnyShift = work.schedule.values.any { it.isNotEmpty() }
            if (!hasAnyShift) {
                Text("No schedule provided.")
            } else {
                WEEK_DAYS.forEach { day ->
                    val shifts = work.schedule[day].orEmpty()
                    if (shifts.isNotEmpty()) {
                        Text(day.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.SemiBold)
                        shifts.forEach { s ->
                            Text("• ${s.start} – ${s.end}", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

private fun openInMaps(context: android.content.Context, work: BudtenderWork) {
    val intent = if (!work.placeId.isNullOrBlank()) {
        // Prefer Place ID and include a label for reliability across devices
        val label = (work.dispensaryName.ifBlank { null } ?: work.address.ifBlank { null }) ?: "Location"
        val url = "https://www.google.com/maps/search/?" +
                "api=1&query=${URLEncoder.encode(label, "UTF-8")}" +
                "&query_place_id=${URLEncoder.encode(work.placeId, "UTF-8")}"
        Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            setPackage("com.google.android.apps.maps")
        }
    } else {
        // Fallback: address/name query
        val query = listOf(work.dispensaryName, work.address)
            .filter { it.isNotBlank() }
            .joinToString(" ")
        val encoded = URLEncoder.encode(query, "UTF-8")
        Intent(Intent.ACTION_VIEW, "geo:0,0?q=$encoded".toUri()).apply {
            setPackage("com.google.android.apps.maps")
        }
    }

    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // If Maps app isn't installed, open in any browser
        intent.setPackage(null)
        context.startActivity(intent)
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
