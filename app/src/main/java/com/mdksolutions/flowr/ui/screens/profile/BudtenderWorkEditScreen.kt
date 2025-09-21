package com.mdksolutions.flowr.ui.screens.profile

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mdksolutions.flowr.R
import com.mdksolutions.flowr.model.WEEK_DAYS
import com.mdksolutions.flowr.model.WorkShift
import com.mdksolutions.flowr.viewmodel.BudtenderWorkViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.Locale

// Places SDK
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudtenderWorkEditScreen(
    navController: NavController,
    vm: BudtenderWorkViewModel = viewModel()
) {
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // One-time Places init using the key in strings.xml
    val placesClient = remember {
        if (!Places.isInitialized()) {
            Places.initialize(context, context.getString(R.string.google_maps_key))
        }
        Places.createClient(context)
    }
    val sessionToken = remember { AutocompleteSessionToken.newInstance() }

    // Draft fields for dispensary info
    var name by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var placeId by rememberSaveable { mutableStateOf("") }

    // Autocomplete UI state
    var nameQuery by rememberSaveable { mutableStateOf("") }
    var nameMenuExpanded by remember { mutableStateOf(false) }
    var predictions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    var placesError by remember { mutableStateOf<String?>(null) } // NEW

    // Keep drafts in sync with loaded data
    LaunchedEffect(ui.work) {
        name = ui.work.dispensaryName
        address = ui.work.address
        placeId = ui.work.placeId.orEmpty()
        nameQuery = name
        predictions = emptyList()
        nameMenuExpanded = false
        placesError = null
    }

    // Debounced query to Places (with visible error)
    LaunchedEffect(nameQuery) {
        val q = nameQuery.trim()
        placesError = null
        if (q.isEmpty()) {
            predictions = emptyList()
            nameMenuExpanded = false
            return@LaunchedEffect
        }
        delay(250)
        try {
            val req = FindAutocompletePredictionsRequest.builder()
                .setQuery(q)
                // .setCountries(listOf("US")) // optional: limit to US
                .setSessionToken(sessionToken)
                .build()
            val result = placesClient.findAutocompletePredictions(req).awaitP()
            predictions = result.autocompletePredictions
            nameMenuExpanded = predictions.isNotEmpty()
        } catch (e: Exception) {
            placesError = e.localizedMessage ?: e.toString()   // show why suggestions failed
            predictions = emptyList()
            nameMenuExpanded = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit Work Info") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        enabled = !ui.isLoading,
                        onClick = {
                            vm.setDispensary(
                                name.trim(),
                                address.trim(),
                                placeId.trim().ifBlank { null }
                            )
                        }
                    ) { Text("Save") }
                }
            )
        }
    ) { padding ->
        if (ui.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text("Dispensary", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    // ---------- NAME with Places Autocomplete ----------
                    ExposedDropdownMenuBox(
                        expanded = nameMenuExpanded,
                        onExpandedChange = { expanded ->
                            nameMenuExpanded = expanded && predictions.isNotEmpty()
                        }
                    ) {
                        OutlinedTextField(
                            value = nameQuery,
                            onValueChange = {
                                nameQuery = it
                                name = it
                                // Reset selection state when typing
                                placeId = ""
                                if (it.isBlank()) {
                                    predictions = emptyList()
                                    nameMenuExpanded = false
                                } else {
                                    nameMenuExpanded = predictions.isNotEmpty()
                                }
                            },
                            label = { Text("Name") },
                            singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = nameMenuExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = nameMenuExpanded && predictions.isNotEmpty(),
                            onDismissRequest = { nameMenuExpanded = false }
                        ) {
                            predictions.forEach { p ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(p.getPrimaryText(null).toString())
                                            val sec = p.getSecondaryText(null).toString()
                                            if (sec.isNotBlank()) {
                                                Text(sec, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    },
                                    onClick = {
                                        nameMenuExpanded = false
                                        fetchPlaceAndFill(
                                            placesClient = placesClient,
                                            placeId = p.placeId,
                                            onDone = { fullName, fullAddress, id ->
                                                name = fullName
                                                nameQuery = fullName
                                                address = fullAddress.orEmpty()
                                                placeId = id.orEmpty()
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Show why suggestions might be missing (key/restriction issues, etc.)
                    if (!placesError.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Places error: $placesError",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // ---------- ADDRESS (auto-filled on selection but editable) ----------
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    // ---------- PLACE ID (auto-filled, optional) ----------
                    OutlinedTextField(
                        value = placeId,
                        onValueChange = { placeId = it },
                        label = { Text("Google Place ID (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                    Text("Weekly Schedule", style = MaterialTheme.typography.titleMedium)
                }

                // One card per day
                items(WEEK_DAYS) { day ->
                    DayEditor(
                        day = day,
                        shifts = ui.work.schedule[day].orEmpty(),
                        onAdd = { start, end -> vm.addShift(day, start, end) },
                        onRemove = { index -> vm.removeShift(day, index) }
                    )
                }

                if (ui.error != null) {
                    item {
                        Text(
                            text = ui.error ?: "",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

/* ------------------------ Day Editor (clock pickers) ------------------------ */

@Composable
private fun DayEditor(
    day: String,
    shifts: List<WorkShift>,
    onAdd: (String, String) -> Unit,
    onRemove: (Int) -> Unit
) {
    val context = LocalContext.current
    var start by rememberSaveable(day) { mutableStateOf<String?>(null) }
    var end by rememberSaveable(day) { mutableStateOf<String?>(null) }

    ElevatedCard {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(day.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.SemiBold)

            if (shifts.isEmpty()) {
                Text("No shifts", style = MaterialTheme.typography.bodySmall)
            } else {
                shifts.forEachIndexed { index, s ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("${s.start} – ${s.end}")
                        IconButton(onClick = { onRemove(index) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove")
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        showTimePicker(
                            context = context,
                            initial = start
                        ) { h, m -> start = formatHHmm(h * 60 + m) }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(start ?: "Start") }

                OutlinedButton(
                    onClick = {
                        showTimePicker(
                            context = context,
                            initial = end
                        ) { h, m -> end = formatHHmm(h * 60 + m) }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(end ?: "End") }

                val canAdd = start != null && end != null &&
                        parseTimeMinutes(start!!)!! < parseTimeMinutes(end!!)!!

                Button(
                    onClick = {
                        onAdd(start!!, end!!)
                        start = null
                        end = null
                    },
                    enabled = canAdd
                ) { Text("Add") }
            }
        }
    }
}

/* ------------------------ Helpers ------------------------ */

private fun showTimePicker(
    context: Context,
    initial: String?,
    is24Hour: Boolean = false,
    onSet: (hour: Int, minute: Int) -> Unit
) {
    val (initH, initM) = initial
        ?.let { parseTimeMinutes(it) }
        ?.let { it / 60 to it % 60 }
        ?: (9 to 0)
    TimePickerDialog(
        context,
        { _, h, m -> onSet(h, m) },
        initH,
        initM,
        is24Hour
    ).show()
}

// Accept "H:mm" or "HH:mm"
private fun parseTimeMinutes(input: String): Int? {
    val m = Regex("""^\s*(\d{1,2}):(\d{2})\s*$""").matchEntire(input) ?: return null
    val h = m.groupValues[1].toIntOrNull() ?: return null
    val min = m.groupValues[2].toIntOrNull() ?: return null
    if (h !in 0..23 || min !in 0..59) return null
    return h * 60 + min
}

// Formats minutes as zero-padded "HH:mm"
private fun formatHHmm(totalMinutes: Int): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return String.format(Locale.US, "%02d:%02d", h, m)
}

// Await wrapper with a distinct name to avoid conflicts with your awaitk()
private suspend fun <T> Task<T>.awaitP(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) cont.resume(task.result)
        else cont.resumeWithException(task.exception ?: RuntimeException("Task failed"))
    }
}

// Fetch place details and pass back name/address/id
private fun fetchPlaceAndFill(
    placesClient: com.google.android.libraries.places.api.net.PlacesClient,
    placeId: String,
    onDone: (name: String, address: String?, id: String?) -> Unit
) {
    val req = FetchPlaceRequest.builder(
        placeId,
        listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS)
    ).build()
    placesClient.fetchPlace(req).addOnSuccessListener { resp ->
        val p = resp.place
        onDone(p.name ?: "", p.address, p.id)
    }.addOnFailureListener {
        // Fallback: still return the id we clicked
        onDone("", null, placeId)
    }
}
