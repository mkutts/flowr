package com.mdksolutions.flowr.ui.screens.productdetail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mdksolutions.flowr.model.Review
import com.mdksolutions.flowr.viewmodel.ProductDetailViewModel
import java.util.Locale

// PATCH: imports for editing & deleting UI
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material3.Slider
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(navController: NavController, productId: String?) {
    val viewModel: ProductDetailViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // current user id for author checks
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // Load product and reviews on first composition
    LaunchedEffect(productId) {
        if (productId != null) {
            viewModel.loadProduct(productId)
            viewModel.loadReviews(productId)
        }
    }

    // Snackbar for errors + status messages
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
                                    val isEditing = uiState.editingReviewId == review.id

                                    if (isEditing) {
                                        // EDIT MODE
                                        OutlinedTextField(
                                            value = uiState.editedText,
                                            onValueChange = { viewModel.setEditedText(it) },
                                            label = { Text("Edit your review") },
                                            modifier = Modifier.fillMaxWidth(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                                        )

                                        Spacer(Modifier.height(8.dp))

                                        // Force rating to stay within 1..5 to satisfy Firestore rules
                                        Slider(
                                            value = uiState.editedRating.coerceIn(1f, 5f),
                                            onValueChange = { viewModel.setEditedRating(it.coerceIn(1f, 5f)) },
                                            valueRange = 1f..5f,
                                            steps = 3
                                        )
                                        Text(text = "Rating: ${"%.1f".format(uiState.editedRating.coerceIn(1f, 5f))}")

                                        Spacer(Modifier.height(8.dp))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Button(
                                                enabled = !uiState.isSavingEdit,
                                                onClick = { viewModel.updateReview() } // uses product + review in uiState
                                            ) {
                                                Text(if (uiState.isSavingEdit) "Saving..." else "Save")
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            TextButton(
                                                enabled = !uiState.isSavingEdit,
                                                onClick = { viewModel.cancelEditingReview() }
                                            ) {
                                                Text("Cancel")
                                            }
                                        }
                                    } else {
                                        // READ-ONLY (your existing card)
                                        ReviewItem(
                                            review = review,
                                            onOpenProfile = { uid ->
                                                navController.navigate("public_profile/$uid")
                                            }
                                        )

                                        // Author actions: Edit + Delete
                                        if (review.userId == currentUserId) {
                                            var confirmDelete by remember(review.id) { mutableStateOf(false) }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                TextButton(
                                                    onClick = {
                                                        viewModel.startEditingReview(
                                                            reviewId = review.id,
                                                            currentText = review.reviewText ?: "",
                                                            currentRating = review.rating
                                                        )
                                                    }
                                                ) { Text("Edit") }

                                                Spacer(Modifier.width(8.dp))

                                                TextButton(
                                                    enabled = !uiState.isDeletingReview,
                                                    colors = ButtonDefaults.textButtonColors(
                                                        contentColor = MaterialTheme.colorScheme.error
                                                    ),
                                                    onClick = { confirmDelete = true }
                                                ) {
                                                    Text(if (uiState.isDeletingReview) "Deleting…" else "Delete")
                                                }
                                            }

                                            if (confirmDelete) {
                                                AlertDialog(
                                                    onDismissRequest = { confirmDelete = false },
                                                    title = { Text("Delete review?") },
                                                    text = { Text("This action cannot be undone.") },
                                                    confirmButton = {
                                                        TextButton(
                                                            enabled = !uiState.isDeletingReview,
                                                            onClick = {
                                                                viewModel.deleteReview(review.id)
                                                                confirmDelete = false
                                                            }
                                                        ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                                                    },
                                                    dismissButton = {
                                                        TextButton(onClick = { confirmDelete = false }) {
                                                            Text("Cancel")
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(16.dp))
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

            // Free-text body with Read more / Read less + clickable @mentions
            if (!review.reviewText.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))

                var expanded by remember(review.id) { mutableStateOf(false) }
                val previewMaxLines = 3
                val body = review.reviewText   // <-- make non-null inside this block

                // Heuristic whether to show "Read more"
                val showToggle = body.lineCountWouldExceed(previewMaxLines)

                // Approximate a preview by characters; if not expanded, show truncated text with ellipsis
                val approxCharsPerLine = 90
                val previewChars = previewMaxLines * approxCharsPerLine
                val displayText =
                    if (expanded || body.length <= previewChars) body
                    else body.take(previewChars).trimEnd() + "…"

                MentionText(
                    text = displayText,
                    onMentionClick = { uid -> onOpenProfile(uid) },
                )

                if (showToggle) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (!expanded) {
                            TextButton(onClick = { expanded = true }) { Text("Read more") }
                        } else {
                            TextButton(onClick = { expanded = false }) { Text("Read less") }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders @[Display Name](uid) tokens as clickable @Display Name that invoke onMentionClick(uid)
 */
@Composable
private fun MentionText(
    text: String,
    onMentionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val mentionRegex = remember { Regex("@\\[(.+?)]\\((.+?)\\)") }

    val annotated: AnnotatedString = remember(text, linkColor) {
        buildAnnotatedString {
            var idx = 0
            for (m in mentionRegex.findAll(text)) {
                val range = m.range
                if (range.first > idx) append(text.substring(idx, range.first))

                val display = m.groupValues[1]
                val uid = m.groupValues[2]

                val start = length
                withStyle(
                    style = SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append("@")
                    append(display)
                }
                addStringAnnotation(
                    tag = "MENTION",
                    annotation = uid,
                    start = start,
                    end = start + display.length + 1 // +1 for '@'
                )

                idx = range.last + 1
            }
            if (idx < text.length) append(text.substring(idx))
        }
    }

    @Suppress("DEPRECATION")
    ClickableText(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
    ) { offset ->
        annotated.getStringAnnotations(tag = "MENTION", start = offset, end = offset)
            .firstOrNull()
            ?.let { onMentionClick(it.item) }
    }
}

/**
 * Lightweight heuristic to decide whether we should show a “Read more” link.
 */
private fun String.lineCountWouldExceed(maxPreviewLines: Int): Boolean {
    val approxCharsPerLine = 90
    return length > maxPreviewLines * approxCharsPerLine
}
