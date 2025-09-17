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
import com.mdksolutions.flowr.model.WEEK_DAYS
import com.mdksolutions.flowr.model.WorkShift
import com.mdksolutions.flowr.viewmodel.BudtenderWorkViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudtenderWorkEditScreen(
    navController: NavController,
    vm: BudtenderWorkViewModel = viewModel()
) {
    val ui by vm.uiState.collectAsStateWithLifecycle()

    // Draft fields for dispensary info
    var name by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var placeId by rememberSaveable { mutableStateOf("") }

    // Keep drafts in sync with loaded data
    LaunchedEffect(ui.work) {
        name = ui.work.dispensaryName
        address = ui.work.address
        placeId = ui.work.placeId.orEmpty()
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
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
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

                // Error message (if any)
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

@Composable
private fun DayEditor(
    day: String,
    shifts: List<WorkShift>,
    onAdd: (String, String) -> Unit,
    onRemove: (Int) -> Unit
) {
    val context = LocalContext.current

    // Store times as normalized "HH:mm"
    var start by rememberSaveable(day) { mutableStateOf<String?>(null) }
    var end by rememberSaveable(day) { mutableStateOf<String?>(null) }

    ElevatedCard {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(day.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.SemiBold)

            // Existing shifts
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

            // Add new shift using clock time pickers
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

/* ---------- Time helpers ---------- */

private fun showTimePicker(
    context: Context,
    initial: String?,
    is24Hour: Boolean = false, // set to true if you prefer 24-hour clock
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

// Accepts "H:mm" or "HH:mm" and returns minutes since midnight
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
    return String.format("%02d:%02d", h, m)
}
