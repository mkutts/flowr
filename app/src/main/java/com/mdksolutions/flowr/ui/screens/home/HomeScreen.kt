package com.mdksolutions.flowr.ui.screens.home

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.mdksolutions.flowr.model.Product
import com.mdksolutions.flowr.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.io.BufferedReader
import com.google.firebase.firestore.FirebaseFirestore
// add:
import java.util.Locale
import androidx.compose.material3.MenuAnchorType

// ⬇️ New imports for handling back to exit app
import androidx.activity.compose.BackHandler
import android.app.Activity
import androidx.compose.material3.HorizontalDivider

// NEW
import androidx.compose.material.icons.filled.Favorite
import com.mdksolutions.flowr.ads.RewardedAds

/* -------------------- State lists & helpers -------------------- */

// Canonical pairs for all 50 states + DC
private val STATE_PAIRS = listOf(
    "AL" to "Alabama", "AK" to "Alaska", "AZ" to "Arizona", "AR" to "Arkansas",
    "CA" to "California", "CO" to "Colorado", "CT" to "Connecticut", "DE" to "Delaware",
    "DC" to "District of Columbia",
    "FL" to "Florida", "GA" to "Georgia", "HI" to "Hawaii", "ID" to "Idaho",
    "IL" to "Illinois", "IN" to "Indiana", "IA" to "Iowa", "KS" to "Kansas",
    "KY" to "Kentucky", "LA" to "Louisiana", "ME" to "Maine", "MD" to "Maryland",
    "MA" to "Massachusetts", "MI" to "Michigan", "MN" to "Minnesota", "MS" to "Mississippi",
    "MO" to "Missouri", "MT" to "Montana", "NE" to "Nebraska", "NV" to "Nevada",
    "NH" to "New Hampshire", "NJ" to "New Jersey", "NM" to "New Mexico", "NY" to "New York",
    "NC" to "North Carolina", "ND" to "North Dakota", "OH" to "Ohio", "OK" to "Oklahoma",
    "OR" to "Oregon", "PA" to "Pennsylvania", "RI" to "Rhode Island", "SC" to "South Carolina",
    "SD" to "South Dakota", "TN" to "Tennessee", "TX" to "Texas", "UT" to "Utah",
    "VT" to "Vermont", "VA" to "Virginia", "WA" to "Washington", "WV" to "West Virginia",
    "WI" to "Wisconsin", "WY" to "Wyoming"
)

private val STATE_ABBR_TO_NAME = STATE_PAIRS.toMap()
private val STATE_NAME_CANON = STATE_PAIRS.associate { it.second.lowercase() to it.second }

// Dropdown uses full names:
private val STATES = listOf("All States") + STATE_PAIRS.map { it.second }

// Categories (unchanged)
private val CATEGORIES = listOf("Flower", "Edible", "Vape", "Other")

// Convert abbreviations or variants to canonical full name
private fun toStateName(value: String): String {
    val v = value.trim()
    if (v.isEmpty()) return v
    // Try abbreviation first (e.g., "CA")
    STATE_ABBR_TO_NAME[v.uppercase()]?.let { return it }
    // Then try full name case-insensitively (proper-case it)
    STATE_NAME_CANON[v.lowercase()]?.let { return it }
    // Accept as-is (for unexpected inputs); still participates in lowercase compare later
    return v
}

/* -------------------- Adjective & Activity dictionaries -------------------- */

// Loads adjectives from assets/adjectives.json (JSON array of strings). Falls back safely.
@Composable
private fun rememberAdjectiveDictionary(): List<String> {
    val context = LocalContext.current
    return remember {
        try {
            val json = context.assets.open("adjectives.json")
                .bufferedReader()
                .use(BufferedReader::readText)
                .trim()

            val words = json
                .removePrefix("[")
                .removeSuffix("]")
                .split(Regex(",\\s*"))
                .mapNotNull { it.trim().trim('"').lowercase().ifBlank { null } }
                .distinct()
                .sorted()

            Log.d("Flowr", "Loaded adjectives: ${words.size}")
            words
        } catch (e: Exception) {
            Log.w("Flowr", "adjectives.json missing or invalid, using fallback. ${e.message}")
            listOf(
                "relaxed","happy","focused","creative","uplifted","sleepy","calm",
                "euphoric","energized","giggly","talkative","motivated","chill",
                "clear-headed","hungry"
            )
        }
    }
}

// Loads activities from assets/activities.json (JSON array of strings). Falls back safely.
@Composable
private fun rememberActivityDictionary(): List<String> {
    val context = LocalContext.current
    return remember {
        try {
            val json = context.assets.open("activities.json")
                .bufferedReader()
                .use(BufferedReader::readText)
                .trim()

            val words = json
                .removePrefix("[")
                .removeSuffix("]")
                .split(Regex(",\\s*"))
                .mapNotNull { it.trim().trim('"').lowercase().ifBlank { null } }
                .distinct()
                .sorted()

            Log.d("Flowr", "Loaded activities: ${words.size}")
            words
        } catch (e: Exception) {
            Log.w("Flowr", "activities.json missing or invalid, using fallback. ${e.message}")
            listOf(
                "gaming","watching tv","nature walk","hiking","reading","painting","cooking","yoga",
                "cycling","meditation","swimming","listening to music","dancing","writing","photography"
            )
        }
    }
}

/* -------------------- Collapsible helper -------------------- */

@Suppress("SameParameterValue") // title is intentionally a param; currently always "Search & Filters"
@Composable
private fun CollapsibleSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    activeCount: Int = 0,
    content: @Composable () -> Unit
) {
    val rotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "arrowRotation")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (activeCount > 0) {
                    AssistChip(
                        onClick = onToggle,
                        label = { Text("$activeCount active") }
                    )
                    Spacer(Modifier.width(8.dp))
                }
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = Icons.Filled.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.rotate(rotation)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    content()
                }
            }
        }
    }
}

/* -------------------- Avg THC helper -------------------- */

@Composable
private fun AvgThcText(
    productId: String,
    initialAvgThc: Double?
) {
    val db = remember { FirebaseFirestore.getInstance() }
    var avg by remember(productId) {
        mutableDoubleStateOf(initialAvgThc?.takeIf { it > 0 } ?: Double.NaN)
    }

    fun numberFromAny(v: Any?): Double? = when (v) {
        null -> null
        is Double -> v
        is Long -> v.toDouble()
        is Int -> v.toDouble()
        is String -> v.toDoubleOrNull()
        else -> null
    }

    fun computeAvg(values: List<Double>): Double =
        if (values.isNotEmpty()) values.average() else Double.NaN

    fun pullThcValues(docs: List<com.google.firebase.firestore.DocumentSnapshot>): List<Double> {
        return docs.mapNotNull { d ->
            val v = numberFromAny(
                d.get("reportedTHC") ?: d.get("thc") ?: d.get("thcPercent")
            )
            v?.takeIf { it in 0.0..100.0 }
        }
    }

    LaunchedEffect(productId) {
        if (avg.isFinite() && avg > 0) return@LaunchedEffect
        db.collection("products").document(productId).collection("reviews")
            .get()
            .addOnSuccessListener { qs ->
                val vals = pullThcValues(qs.documents)
                val a = computeAvg(vals)
                if (a.isFinite()) {
                    avg = a
                } else {
                    db.collection("reviews")
                        .whereEqualTo("productId", productId)
                        .get()
                        .addOnSuccessListener { qs2 ->
                            val vals2 = pullThcValues(qs2.documents)
                            val a2 = computeAvg(vals2)
                            if (a2.isFinite()) avg = a2
                        }
                        .addOnFailureListener { /* ignore */ }
                }
            }
            .addOnFailureListener {
                db.collection("reviews")
                    .whereEqualTo("productId", productId)
                    .get()
                    .addOnSuccessListener { qs2 ->
                        val vals2 = pullThcValues(qs2.documents)
                        val a2 = computeAvg(vals2)
                        if (a2.isFinite()) avg = a2
                    }
            }
    }

    val text = if (avg.isFinite()) String.format(Locale.US, "%.1f%%", avg) else "—"
    Text(text = "Avg THC: $text")
}

/* -------------------- Public User Search -------------------- */

// Small model for search results
private data class UserHit(
    val uid: String,
    val displayName: String,
    val handle: String? = null,
    val photoUrl: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserSearchBar(navController: NavController) {
    val db = remember { FirebaseFirestore.getInstance() }

    var query by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf(emptyList<UserHit>()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        val raw = query.trim()
        if (raw.isEmpty()) {
            results = emptyList()
            return@LaunchedEffect
        }
        isLoading = true

        val qLower = raw.lowercase()
        val handleLower = if (raw.startsWith("@")) raw.drop(1).lowercase() else qLower

        suspend fun tryFetchBy(fieldPreferred: String, fieldFallback: String?, textLower: String): List<UserHit> {
            val preferred = try {
                val snap = db.collection("users")
                    .orderBy(fieldPreferred)
                    .startAt(textLower)
                    .endAt(textLower + "\uf8ff")
                    .limit(15)
                    .get()
                    .await()
                snap.documents.mapNotNull { d ->
                    val uid = d.id
                    val name = (d.get("displayName") as? String)?.ifBlank { null } ?: return@mapNotNull null
                    val handle = (d.get("username") as? String)?.ifBlank { null }
                    val photo = (d.get("photoUrl") as? String)?.ifBlank { null }
                    UserHit(uid, name, handle, photo)
                }
            } catch (_: Exception) { emptyList() }

            if (preferred.isNotEmpty()) return preferred

            if (fieldFallback == null) return emptyList()
            return try {
                val snap = db.collection("users")
                    .orderBy(fieldFallback)
                    .startAt(raw)
                    .endAt(raw + "\uf8ff")
                    .limit(25)
                    .get()
                    .await()
                snap.documents.mapNotNull { d ->
                    val uid = d.id
                    val name = (d.get("displayName") as? String)?.ifBlank { null } ?: return@mapNotNull null
                    val handle = (d.get("username") as? String)?.ifBlank { null }
                    val photo = (d.get("photoUrl") as? String)?.ifBlank { null }
                    UserHit(uid, name, handle, photo)
                }.filter { hit ->
                    hit.displayName.lowercase().startsWith(textLower) ||
                            (hit.handle?.lowercase()?.startsWith(textLower) == true)
                }
            } catch (_: Exception) { emptyList() }
        }

        // name search unchanged
        val nameHits = tryFetchBy("displayName_lc", "displayName", qLower)
        // handle → username search (uses existing usernameLower field)
        val handleHits = tryFetchBy("usernameLower", "username", handleLower)

        results = (nameHits + handleHits)
            .distinctBy { it.uid }
            .sortedBy { it.displayName.lowercase() }

        isLoading = false
    }

    // --- NEW API: DockedSearchBar with inputField slot ---
    DockedSearchBar(
        expanded = active,
        onExpandedChange = { isActive -> active = isActive },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = { query = it },
                onSearch = { q ->
                    query = q
                    active = false
                },
                expanded = active,
                onExpandedChange = { isActive -> active = isActive },
                placeholder = { Text("Search users by name or @username") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                }
            )
        }
    ) {
        if (isLoading && results.isEmpty()) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        results.forEach { hit ->
            ListItem(
                headlineContent = { Text(hit.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = { hit.handle?.let { Text("@$it") } },
                leadingContent = {
                    if (!hit.photoUrl.isNullOrBlank()) {
                        Image(
                            painter = rememberAsyncImagePainter(hit.photoUrl),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(20.dp))
                        )
                    } else {
                        Icon(imageVector = Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(40.dp))
                    }
                },
                modifier = Modifier.clickable {
                    navController.navigate("public_profile/${hit.uid}")
                    results = emptyList(); query = ""; active = false
                }
            )
            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        }
        if (!isLoading && results.isEmpty() && query.isNotBlank()) {
            Text("No users found", modifier = Modifier.padding(16.dp).fillMaxWidth())
        }
    }
}

/* -------------------- Home Screen -------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    backStackEntry: NavBackStackEntry
) {
    val viewModel: HomeViewModel = viewModel(backStackEntry)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val activity = LocalContext.current as? Activity
    BackHandler { activity?.finish() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val states = remember { STATES }
    val categories = remember { CATEGORIES }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Flowr") },
                    actions = {
                        IconButton(onClick = { navController.navigate("profile") }) {
                            Icon(Icons.Filled.Person, contentDescription = "Profile")
                        }
                    }
                )
                // 🔎 Public profile search
                UserSearchBar(navController = navController)
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            val act = LocalContext.current as? Activity
            var showAdPrompt by remember { mutableStateOf(false) }

            // The FAB
            FloatingActionButton(
                onClick = { showAdPrompt = true }
            ) {
                Icon(Icons.Filled.Favorite, contentDescription = "I love Flowr")
            }

            // Confirm dialog
            if (showAdPrompt) {
                AlertDialog(
                    onDismissRequest = { showAdPrompt = false },
                    title = { Text("Support Flowr") },
                    text = { Text("Help keep our servers up by watching an ad.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showAdPrompt = false
                                act?.let { a ->
                                    RewardedAds.show(a) { /* reward -> handle if you want */ }
                                }
                            }
                        ) { Text("Watch Ad") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAdPrompt = false }) { Text("Not now") }
                    }
                )
            }
        }

    ) { padding ->
        // shrink bottom padding so content sits closer to the bottom nav
        val layoutDirection = LocalLayoutDirection.current
        val adjustedPadding = PaddingValues(
            start = padding.calculateStartPadding(layoutDirection),
            top = padding.calculateTopPadding(),
            end = padding.calculateEndPadding(layoutDirection),
            bottom = 8.dp
        )

        Column(
            modifier = Modifier
                .padding(adjustedPadding)
                .fillMaxSize()
        ) {
            // ── Collapsible Search & Filters ────────────────────────────────
            var filtersExpanded by rememberSaveable { mutableStateOf(false) }

            val activeFilters = remember(
                uiState.searchQuery,
                uiState.selectedState,
                uiState.selectedCategory,
                uiState.selectedFeel,
                uiState.selectedActivity
            ) {
                listOfNotNull(
                    uiState.searchQuery.takeIf { it.isNotBlank() },
                    uiState.selectedState?.takeIf { it != "All States" },
                    uiState.selectedCategory,
                    uiState.selectedFeel,
                    uiState.selectedActivity
                ).size
            }

            CollapsibleSection(
                title = "Search & Filters",
                isExpanded = filtersExpanded,
                onToggle = { filtersExpanded = !filtersExpanded },
                activeCount = activeFilters
            ) {
                // ✅ Debounced Search Bar
                var searchDraft by remember { mutableStateOf(uiState.searchQuery) }
                LaunchedEffect(searchDraft) {
                    delay(200)
                    if (searchDraft != uiState.searchQuery) {
                        viewModel.updateSearchQuery(searchDraft)
                    }
                }

                OutlinedTextField(
                    value = searchDraft,
                    onValueChange = { searchDraft = it },
                    label = { Text("Search by name or brand") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                // ✅ State Dropdown Filter (full names)
                var stateMenuExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = stateMenuExpanded,
                    onExpandedChange = { stateMenuExpanded = !stateMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = uiState.selectedState ?: "All States",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Filter by State") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateMenuExpanded)
                        },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = stateMenuExpanded,
                        onDismissRequest = { stateMenuExpanded = false }
                    ) {
                        states.forEach { state ->
                            DropdownMenuItem(
                                text = { Text(state) },
                                onClick = {
                                    viewModel.updateState(if (state == "All States") null else state)
                                    stateMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // ✅ Category Filter Chips
                Text("Filter by Category", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = uiState.selectedCategory == category,
                            onClick = {
                                viewModel.updateCategory(
                                    if (uiState.selectedCategory == category) null else category
                                )
                            },
                            label = { Text(category) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // ✅ Feels Type-Ahead
                Text("Filter by Feel", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))

                val adjectives = rememberAdjectiveDictionary()
                var feelsDraft by remember { mutableStateOf(uiState.selectedFeel ?: "") }
                var feelsMenuExpanded by remember { mutableStateOf(false) }

                LaunchedEffect(feelsDraft) {
                    delay(200)
                    val clean = feelsDraft.trim()
                    if (clean.isEmpty()) {
                        if (uiState.selectedFeel != null) viewModel.updateFeel(null)
                    } else if (!uiState.selectedFeel.equals(clean, ignoreCase = true)) {
                        viewModel.updateFeel(clean)
                    }
                }

                val feelSuggestions by remember(feelsDraft, adjectives) {
                    derivedStateOf {
                        val q = feelsDraft.trim().lowercase()
                        if (q.isEmpty()) emptyList()
                        else {
                            val starts = adjectives.filter { it.startsWith(q) }
                            val contains = adjectives.filter { it.contains(q) && !it.startsWith(q) }
                            (starts + contains).take(8)
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = feelsMenuExpanded && feelSuggestions.isNotEmpty(),
                    onExpandedChange = { expanded ->
                        feelsMenuExpanded = if (feelSuggestions.isNotEmpty()) expanded else false
                    }
                ) {
                    OutlinedTextField(
                        value = feelsDraft,
                        onValueChange = {
                            feelsDraft = it
                            feelsMenuExpanded = true
                        },
                        label = { Text("Type a feel (adjective)") },
                        trailingIcon = {
                            Row {
                                if (feelsDraft.isNotEmpty()) {
                                    TextButton(onClick = {
                                        feelsDraft = ""
                                        feelsMenuExpanded = false
                                    }) { Text("Clear") }
                                }
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = feelsMenuExpanded && feelSuggestions.isNotEmpty()
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = feelsMenuExpanded && feelSuggestions.isNotEmpty(),
                        onDismissRequest = { feelsMenuExpanded = false }
                    ) {
                        feelSuggestions.forEach { word ->
                            DropdownMenuItem(
                                text = { Text(word) },
                                onClick = {
                                    feelsDraft = word
                                    viewModel.updateFeel(word)
                                    feelsMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ✅ Activities Type-Ahead
                Text("Filter by Activity", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))

                val activitiesDict = rememberActivityDictionary()
                var activityDraft by remember { mutableStateOf(uiState.selectedActivity ?: "") }
                var activityMenuExpanded by remember { mutableStateOf(false) }

                LaunchedEffect(activityDraft) {
                    delay(200)
                    val clean = activityDraft.trim()
                    if (clean.isEmpty()) {
                        if (uiState.selectedActivity != null) viewModel.updateActivity(null)
                    } else if (!uiState.selectedActivity.equals(clean, ignoreCase = true)) {
                        viewModel.updateActivity(clean)
                    }
                }

                val activitySuggestions by remember(activityDraft, activitiesDict) {
                    derivedStateOf {
                        val q = activityDraft.trim().lowercase()
                        if (q.isEmpty()) emptyList()
                        else {
                            val starts = activitiesDict.filter { it.startsWith(q) }
                            val contains = activitiesDict.filter { it.contains(q) && !it.startsWith(q) }
                            (starts + contains).take(8)
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = activityMenuExpanded && activitySuggestions.isNotEmpty(),
                    onExpandedChange = { expanded ->
                        activityMenuExpanded = if (activitySuggestions.isNotEmpty()) expanded else false
                    }
                ) {
                    OutlinedTextField(
                        value = activityDraft,
                        onValueChange = {
                            activityDraft = it
                            activityMenuExpanded = true
                        },
                        label = { Text("Type an activity") },
                        trailingIcon = {
                            Row {
                                if (activityDraft.isNotEmpty()) {
                                    TextButton(onClick = {
                                        activityDraft = ""
                                        activityMenuExpanded = false
                                    }) { Text("Clear") }
                                }
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = activityMenuExpanded && activitySuggestions.isNotEmpty()
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = activityMenuExpanded && activitySuggestions.isNotEmpty(),
                        onDismissRequest = { activityMenuExpanded = false }
                    ) {
                        activitySuggestions.forEach { act ->
                            DropdownMenuItem(
                                text = { Text(act) },
                                onClick = {
                                    activityDraft = act
                                    viewModel.updateActivity(act)
                                    activityMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            } // end Collapsible content

            // ── Product list (focus area) ──────────────────────────────────
            Spacer(Modifier.height(4.dp))

            // ✅ Memoized filtering (now normalizes state names/abbr)
            val filteredProducts by remember(
                uiState.products,
                uiState.searchQuery,
                uiState.selectedCategory,
                uiState.selectedState,
                uiState.selectedFeel,
                uiState.selectedActivity
            ) {
                derivedStateOf {
                    val q = uiState.searchQuery.trim().lowercase()
                    val cat = uiState.selectedCategory?.lowercase()
                    val st = uiState.selectedState?.takeUnless { it == "All States" }?.let { toStateName(it).lowercase() }
                    val feel = uiState.selectedFeel
                    val activity = uiState.selectedActivity

                    uiState.products.filter { p ->
                        val nameL = p.name.lowercase()
                        val brandL = p.brand.lowercase()
                        val catL = p.category.lowercase()
                        val stateNameL = toStateName(p.state).lowercase() // normalize product state too

                        (q.isEmpty() || nameL.contains(q) || brandL.contains(q)) &&
                                (
                                        cat == null ||
                                                (cat == "other" && catL !in listOf("flower", "vape", "edible", "concentrate", "pre-roll", "pre roll")) ||
                                                (cat != "other" && catL == cat)
                                        ) &&
                                (st == null || stateNameL == st) &&
                                (feel == null || p.topFeels.any { it.equals(feel, ignoreCase = true) }) &&
                                (activity == null || p.topActivities.any { it.equals(activity, ignoreCase = true) })
                    }
                }
            }

            // ✅ UI State Handling
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(6) { ProductPlaceholder() }
                        }
                    }
                    filteredProducts.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No products match your filters")
                        }
                    }
                    else -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(
                                items = filteredProducts,
                                key = { it.id },
                                contentType = { "product_row" }
                            ) { product ->
                                ProductItem(product) {
                                    navController.navigate("product_detail/${product.id}")
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
fun ProductItem(product: Product, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = product.name, style = MaterialTheme.typography.titleMedium)
            Text(text = "Brand: ${product.brand}")
            Text(text = "Category: ${product.category}")

            // ✅ PATCH: Display multiple states or fallback to legacy single state
            val statesText = when {
                product.states != null && product.states.isNotEmpty() ->
                    product.states.joinToString(", ") { toStateName(it) }
                product.state.isNotBlank() -> toStateName(product.state)
                else -> "—"
            }
            Text(text = "States: $statesText")

            if (product.strainType.isNotBlank()) {
                Text(text = "Strain: ${product.strainType}")
            }
            if (product.topFeels.isNotEmpty()) {
                Text(text = "Feels: ${product.topFeels.joinToString(", ")}")
            }
            if (product.topActivities.isNotEmpty()) {
                Text(text = "Activities: ${product.topActivities.joinToString(", ")}")
            }

            AvgThcText(productId = product.id, initialAvgThc = product.avgTHC)
        }
    }
}

@Composable
fun ProductPlaceholder() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .height(20.dp)
                    .fillMaxWidth(0.6f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.LightGray.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .height(16.dp)
                    .fillMaxWidth(0.4f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.LightGray.copy(alpha = 0.4f))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .height(16.dp)
                    .fillMaxWidth(0.5f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.LightGray.copy(alpha = 0.4f))
            )
        }
    }
}
