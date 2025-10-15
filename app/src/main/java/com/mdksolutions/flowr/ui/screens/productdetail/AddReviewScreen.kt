package com.mdksolutions.flowr.ui.screens.productdetail

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.mdksolutions.flowr.model.Review
import com.mdksolutions.flowr.viewmodel.ProductDetailViewModel
import com.mdksolutions.flowr.viewmodel.FollowingViewModel
import com.mdksolutions.flowr.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import androidx.core.content.edit
import java.util.Locale // ✅ for category check

// ==== Storage for custom words (can't write to assets at runtime) ====
private const val PREFS_NAME = "custom_words_prefs"
private const val PREFS_KEY_FEELS = "custom_feels"
private const val PREFS_KEY_ACTIVITIES = "custom_activities"

private fun getCustomFeels(context: Context): List<String> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = prefs.getString(PREFS_KEY_FEELS, "[]") ?: "[]"
    val arr = JSONArray(json)
    return List(arr.length()) { i -> arr.getString(i) }
}

private fun saveCustomFeels(context: Context, words: List<String>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val arr = JSONArray(words)
    prefs.edit { putString(PREFS_KEY_FEELS, arr.toString()) }
}

private fun getCustomActivities(context: Context): List<String> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = prefs.getString(PREFS_KEY_ACTIVITIES, "[]") ?: "[]"
    val arr = JSONArray(json)
    return List(arr.length()) { i -> arr.getString(i) }
}

private fun saveCustomActivities(context: Context, words: List<String>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val arr = JSONArray(words)
    prefs.edit { putString(PREFS_KEY_ACTIVITIES, arr.toString()) }
}
// ====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReviewScreen(navController: NavController, productId: String?) {
    val viewModel: ProductDetailViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val auth = FirebaseAuth.getInstance()
    val snackbarHostState = remember { SnackbarHostState() }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    // ✅ Load product so we can detect category (mg vs %)
    LaunchedEffect(productId) {
        if (productId != null) viewModel.loadProduct(productId)
    }

    // ✅ Form state (survives config changes)
    var rating by rememberSaveable { mutableStateOf("") }
    var feels by rememberSaveable { mutableStateOf("") }
    var activity by rememberSaveable { mutableStateOf("") }

    // 🔁 Replaces old "thc" field with adaptive potency text
    var potencyText by rememberSaveable { mutableStateOf("") }

    // ✅ NEW: free-text review state
    var reviewText by rememberSaveable { mutableStateOf("") }
    val reviewCharLimit = 2000
    val minCharsToSubmit = 12
    val remainingChars = reviewCharLimit - reviewText.length

    // ✅ NEW: tagging state
    var showTagDialog by rememberSaveable { mutableStateOf(false) }
    var tagSearch by rememberSaveable { mutableStateOf("") }

    val context = LocalContext.current

    // ===== Feels dict: base (assets) + custom (prefs)
    var adjectives by remember { mutableStateOf<List<String>>(emptyList()) }
    // use SnapshotStateList to avoid "mutable collection" warning
    val customFeels: SnapshotStateList<String> = remember {
        mutableStateListOf<String>().apply { addAll(getCustomFeels(context)) }
    }

    // ===== Activities dict: base (assets) + custom (prefs)
    var activities by remember { mutableStateOf<List<String>>(emptyList()) }
    val customActivities: SnapshotStateList<String> = remember {
        mutableStateListOf<String>().apply { addAll(getCustomActivities(context)) }
    }

    // Load both dictionaries
    LaunchedEffect(Unit) {
        try {
            val baseFeels = withContext(Dispatchers.IO) {
                val json = loadJsonFromAssets(context, "adjectives.json")
                val arr = JSONArray(json)
                List(arr.length()) { i -> arr.getString(i) }
            }
            adjectives = (baseFeels + customFeels).distinct().sorted()
        } catch (e: Exception) {
            snackbarHostState.showSnackbar("Couldn't load feels list: ${e.message ?: "Unknown error"}")
        }

        try {
            val baseActivities = withContext(Dispatchers.IO) {
                val json = loadJsonFromAssets(context, "activities.json") // <-- assets/activities.json
                val arr = JSONArray(json)
                List(arr.length()) { i -> arr.getString(i) }
            }
            activities = (baseActivities + customActivities).distinct().sorted()
        } catch (_: Exception) {
            // If you don't have activities.json yet, keep the list empty and still work.
            activities = customActivities.distinct().sorted()
        }
    }

    // Decide potency unit based on category
    val usesMg by remember(uiState.product?.category) {
        mutableStateOf(
            (uiState.product?.category?.lowercase(Locale.US) ?: "").let { cat ->
                cat.contains("edible") || cat.contains("drink")
            }
        )
    }

    // ===== Feels autocomplete (comma-separated tokens)
    val currentFeelsToken by remember(feels) {
        derivedStateOf { feels.substringAfterLast(",", feels).trim() }
    }
    val feelSuggestions by remember(currentFeelsToken, adjectives) {
        derivedStateOf {
            if (currentFeelsToken.isBlank()) emptyList()
            else adjectives.filter { it.startsWith(currentFeelsToken, ignoreCase = true) }.take(8)
        }
    }
    var feelsExpanded by remember { mutableStateOf(false) }

    // ===== Activity autocomplete (single phrase)
    val activitySuggestions by remember(activity, activities) {
        derivedStateOf {
            if (activity.isBlank()) emptyList()
            else activities.filter { it.startsWith(activity.trim(), ignoreCase = true) }.take(8)
        }
    }
    var activityExpanded by remember { mutableStateOf(false) }

    // ✅ Snackbar for ViewModel messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // ✅ Navigate back on successful review submission
    LaunchedEffect(uiState.reviewAdded) {
        if (uiState.reviewAdded) {
            navController.popBackStack()
            viewModel.clearReviewAdded()
        }
    }

    // ✅ Show Snackbar for validation errors
    LaunchedEffect(validationMessage) {
        validationMessage?.let {
            snackbarHostState.showSnackbar(it)
            validationMessage = null
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Add Review") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Add Your Review", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = rating,
                onValueChange = { rating = it },
                label = { Text("Rating (1-5)") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // ==== Feels with autocomplete (comma-separated) ====
            ExposedDropdownMenuBox(
                expanded = feelsExpanded && feelSuggestions.isNotEmpty(),
                onExpandedChange = { wantOpen ->
                    feelsExpanded = wantOpen && feelSuggestions.isNotEmpty()
                }
            ) {
                OutlinedTextField(
                    value = feels,
                    onValueChange = { newVal ->
                        feels = newVal
                        feelsExpanded = currentFeelsToken.isNotBlank() && feelSuggestions.isNotEmpty()
                    },
                    label = { Text("Feels (comma separated)") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = feelsExpanded && feelSuggestions.isNotEmpty(),
                    onDismissRequest = { feelsExpanded = false }
                ) {
                    feelSuggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion) },
                            onClick = {
                                val parts = feels.split(",")
                                    .map { it.trim() }
                                    .toMutableList()
                                val endsWithComma = feels.trim().endsWith(",")
                                if (parts.isEmpty() || (parts.size == 1 && parts.first().isBlank())) {
                                    parts.clear()
                                    parts.add(suggestion)
                                } else if (endsWithComma) {
                                    parts.add(suggestion)
                                } else if (parts.isNotEmpty()) {
                                    parts[parts.lastIndex] = suggestion
                                } else {
                                    parts.add(suggestion)
                                }
                                feels = parts.joinToString(", ") + ", "
                                feelsExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==== Activity with autocomplete (single phrase) ====
            ExposedDropdownMenuBox(
                expanded = activityExpanded && activitySuggestions.isNotEmpty(),
                onExpandedChange = { wantOpen ->
                    activityExpanded = wantOpen && activitySuggestions.isNotEmpty()
                }
            ) {
                OutlinedTextField(
                    value = activity,
                    onValueChange = { newVal ->
                        activity = newVal
                        activityExpanded = activity.isNotBlank() && activitySuggestions.isNotEmpty()
                    },
                    label = { Text("Makes me want to...") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = activityExpanded && activitySuggestions.isNotEmpty(),
                    onDismissRequest = { activityExpanded = false }
                ) {
                    activitySuggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion) },
                            onClick = {
                                activity = "$suggestion "
                                activityExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ✅ Adaptive potency input: mg for edibles/drinks, % for others
            OutlinedTextField(
                value = potencyText,
                onValueChange = { input ->
                    val cleaned = input.replace("[^0-9.]".toRegex(), "")
                    potencyText = cleaned
                },
                label = { Text(if (usesMg) "Dosage (mg per serving) - Optional" else "Reported THC (%) - Optional") },
                placeholder = { Text(if (usesMg) "e.g. 10" else "e.g. 18.5") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ✅ Free-text review box
            OutlinedTextField(
                value = reviewText,
                onValueChange = { new ->
                    reviewText = if (new.length <= reviewCharLimit) new else new.take(reviewCharLimit)
                },
                label = { Text("Your words") },
                placeholder = { Text("Tell others what you liked, effects, taste, and any caveats…") },
                supportingText = {
                    val tooShort = reviewText.trim().length in 1 until minCharsToSubmit
                    val msg = when {
                        reviewText.isBlank() -> "Optional, but helpful."
                        tooShort -> "A few more words makes this useful (min $minCharsToSubmit chars)."
                        else -> "$remainingChars characters left"
                    }
                    Text(msg)
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
                maxLines = 12
            )

            // ✅ NEW: Tag someone button + dialog
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { showTagDialog = true }) {
                Text("Tag someone")
            }

            if (showTagDialog) {
                val followingVm: FollowingViewModel = viewModel()
                val followingState by followingVm.uiState.collectAsState()
                // NOTE: removed followingVm.loadFollowing() to avoid unresolved reference error.
                // If your VM doesn't auto-load, add the fetch in its init{}.

                AlertDialog(
                    onDismissRequest = { showTagDialog = false },
                    title = { Text("Tag someone") },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = tagSearch,
                                onValueChange = { tagSearch = it },
                                label = { Text("Search") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))

                            val list = (followingState.users.takeIf { it.isNotEmpty() } ?: emptyList())
                                .filter { it.displayName.contains(tagSearch, ignoreCase = true) }

                            if (followingState.isLoading) {
                                CircularProgressIndicator()
                            } else {
                                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                                    items(list) { u: UserProfile ->
                                        ListItem(
                                            headlineContent = { Text(u.displayName) },
                                            supportingContent = { Text(u.email) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    // Insert token: @[Display Name](uid)
                                                    val token = " @[${u.displayName}](${u.uid}) "
                                                    reviewText = (reviewText + token).trim()
                                                    showTagDialog = false
                                                    tagSearch = ""
                                                }
                                        )
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showTagDialog = false }) { Text("Close") }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isSubmittingReview) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        // Close menus so the click isn't eaten
                        feelsExpanded = false
                        activityExpanded = false

                        val user = auth.currentUser
                        if (productId == null) {
                            validationMessage = "Missing product ID"
                            return@Button
                        }
                        if (user == null) {
                            validationMessage = "You must be logged in to submit a review"
                            return@Button
                        }
                        val ratingVal = rating.toIntOrNull()
                        if (ratingVal == null || ratingVal !in 1..5) {
                            validationMessage = "Please enter a rating from 1 to 5"
                            return@Button
                        }
                        if (activity.isBlank()) {
                            validationMessage = "Please fill in the activity"
                            return@Button
                        }
                        // Soft-require meaningful text if they started typing
                        val cleanedReviewText = reviewText.trim()
                        if (cleanedReviewText.isNotEmpty() && cleanedReviewText.length < minCharsToSubmit) {
                            validationMessage = "Please add a bit more detail to your review"
                            return@Button
                        }

                        val feelsList = feels.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }

                        try {
                            // Persist any new FEELS
                            val feelsLower = adjectives.map { it.lowercase() }.toSet()
                            val missingFeels = feelsList.filter { it.lowercase() !in feelsLower }
                            if (missingFeels.isNotEmpty()) {
                                customFeels.addAll(missingFeels.filterNot { it in customFeels })
                                saveCustomFeels(context, customFeels)
                                adjectives = (adjectives + missingFeels).distinct().sorted()
                            }

                            // Persist any new ACTIVITY (whole phrase)
                            val actLower = activities.map { it.lowercase() }.toSet()
                            val actVal = activity.trim()
                            if (actVal.isNotEmpty() && actVal.lowercase() !in actLower) {
                                if (actVal !in customActivities) customActivities.add(actVal)
                                saveCustomActivities(context, customActivities)
                                activities = (activities + actVal).distinct().sorted()
                            }

                            // ✅ Parse potency based on category
                            val parsed = potencyText.toDoubleOrNull()
                            val safePotency = when {
                                parsed == null -> null
                                usesMg -> parsed.coerceIn(0.0, 1000.0)   // mg sane cap
                                else   -> parsed.coerceIn(0.0, 100.0)    // % cap
                            }

                            val review = Review(
                                productId = productId,
                                userId = user.uid,
                                userName = user.email ?: "Anonymous",
                                rating = ratingVal,
                                feels = feelsList,
                                activity = actVal,
                                // ✅ write only one of these:
                                reportedTHC = if (!usesMg) safePotency else null,
                                dosageMg = if (usesMg) safePotency else null,
                                reviewText = cleanedReviewText.ifEmpty { null }
                            )

                            viewModel.addReview(review)
                        } catch (e: Exception) {
                            validationMessage = "Failed to submit review: ${e.message ?: "Unknown error"}"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Submit Review")
                }
            }
        }
    }
}

// ✅ Helper to load JSON from assets
fun loadJsonFromAssets(context: Context, fileName: String): String {
    return context.assets.open(fileName).bufferedReader().use { it.readText() }
}
