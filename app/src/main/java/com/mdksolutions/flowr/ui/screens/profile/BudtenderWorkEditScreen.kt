package com.mdksolutions.flowr.ui.screens.profile

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
    // Local drafts for a new shift on this day
    var start by rememberSaveable(day) { mutableStateOf("") }
    var end by rememberSaveable(day) { mutableStateOf("") }

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
                            Icon(Icons.Default.Delete, contentDescription = "Remove")
                        }
                    }
                }
            }

            // Add new shift
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = start,
                    onValueChange = { start = it },
                    label = { Text("Start (HH:mm)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = end,
                    onValueChange = { end = it },
                    label = { Text("End (HH:mm)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        if (isValidTimeWindow(start, end)) {
                            onAdd(start.trim(), end.trim())
                            start = ""
                            end = ""
                        }
                    },
                    enabled = isValidTimeWindow(start, end)
                ) { Text("Add") }
            }
        }
    }
}

private fun isValidTimeWindow(start: String, end: String): Boolean {
    val rx = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
    if (!rx.matches(start.trim()) || !rx.matches(end.trim())) return false
    fun toMinutes(t: String): Int {
        val h = t.substring(0, 2).toInt()
        val m = t.substring(3, 5).toInt()
        return h * 60 + m
    }
    return toMinutes(start.trim()) < toMinutes(end.trim())
}
