package com.mdksolutions.flowr.ui.screens.productdetail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mdksolutions.flowr.model.Review
import com.mdksolutions.flowr.viewmodel.ProductDetailViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(navController: NavController, productId: String?) {
    val viewModel: ProductDetailViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load product and reviews on first composition
    LaunchedEffect(productId) {
        if (productId != null) {
            viewModel.loadProduct(productId)
            viewModel.loadReviews(productId)
        }
    }

    // Snackbar for errors
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // Navigate back after review submit
    LaunchedEffect(uiState.reviewAdded) {
        if (uiState.reviewAdded) {
            navController.popBackStack()
            viewModel.clearReviewAdded()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_review/$productId") }) {
                Icon(Icons.Default.Add, contentDescription = "Add Review")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when {
                uiState.isLoadingProduct -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.product == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Product not found")
                    }
                }
                else -> {
                    val product = uiState.product!!

                    // --- compute averages from the reviews currently loaded ---
                    val reviews = uiState.reviews

                    val avgThcFromReviews by remember(reviews) {
                        mutableStateOf(
                            reviews.mapNotNull { it.reportedTHC }
                                .takeIf { it.isNotEmpty() }
                                ?.average()
                        )
                    }
                    val avgRatingFromReviews by remember(reviews) {
                        mutableStateOf(
                            reviews.map { it.rating.toDouble() }
                                .takeIf { it.isNotEmpty() }
                                ?.average()
                        )
                    }

                    // Format helpers with explicit Locale
                    val avgThcText = avgThcFromReviews?.let { String.format(Locale.US, "%.1f%%", it) } ?: "0.0%"
                    val avgRatingText = avgRatingFromReviews?.let { String.format(Locale.US, "%.1f", it) } ?: "0.0"

                    Text(text = product.name, style = MaterialTheme.typography.titleLarge)
                    Text(text = "Brand: ${product.brand}")
                    Text(text = "Category: ${product.category}")

                    // Use computed averages rather than the (possibly stale) product fields
                    Text(text = "Avg THC: $avgThcText")
                    Text(text = "Avg Rating: $avgRatingText")

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Reviews", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    when {
                        uiState.isLoadingReviews -> CircularProgressIndicator()
                        uiState.reviews.isEmpty() -> Text("No reviews yet. Be the first!")
                        else -> {
                            LazyColumn {
                                items(uiState.reviews) { review ->
                                    ReviewItem(
                                        review = review,
                                        onOpenProfile = { uid ->
                                            navController.navigate("public_profile/$uid")
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewItem(
    review: Review,
    onOpenProfile: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize()
        ) {
            AssistChip(
                onClick = { onOpenProfile(review.userId) },
                label = { Text("View reviewer") }
            )

            Spacer(Modifier.height(8.dp))

            Text(text = "Rating: ${review.rating} ⭐")
            Text(text = "Feels: ${review.feels.joinToString(", ")}")
            Text(text = "Activity: ${review.activity}")
            review.reportedTHC?.let {
                Text(text = "Reported THC: $it%")
            }

            // ✅ Free-text body with Read more / Read less
            if (!review.reviewText.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))

                var expanded by remember(review.id) { mutableStateOf(false) }
                val previewMaxLines = 3
                val body = review.reviewText

                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (expanded) Int.MAX_VALUE else previewMaxLines,
                    overflow = TextOverflow.Ellipsis
                )

                // Show toggle only if content would overflow the preview
                if (!expanded && body.lineCountWouldExceed(previewMaxLines)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { expanded = true }) {
                            Text("Read more")
                        }
                    }
                } else if (expanded) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { expanded = false }) {
                            Text("Read less")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Lightweight heuristic to decide whether we should show a “Read more” link.
 * We can’t measure layout here, so we approximate:
 * - If the text length is large enough, assume it will exceed N lines in most devices.
 * - Tuned so typical ~70–90 chars/line at body text will need ~3 lines past ~300 chars.
 */
private fun String.lineCountWouldExceed(maxPreviewLines: Int): Boolean {
    val approxCharsPerLine = 90 // conservative for body text on phones
    return length > maxPreviewLines * approxCharsPerLine
}
