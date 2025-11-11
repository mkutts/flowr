package com.mdksolutions.flowr.ui.screens.support

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mdksolutions.flowr.viewmodel.SuggestionViewModel
import androidx.compose.material3.MenuAnchorType // ⬅️ NEW import

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionScreen(
    navController: NavController,
    vm: SuggestionViewModel = viewModel()
) {
    val ui by vm.ui.collectAsState()
    val ctx = LocalContext.current
    val scroll = rememberScrollState()

    LaunchedEffect(ui.successId, ui.error) {
        ui.successId?.let {
            Toast.makeText(ctx, "Thanks! Submitted (#$it)", Toast.LENGTH_LONG).show()
            navController.popBackStack()
        }
        ui.error?.let {
            Toast.makeText(ctx, it, Toast.LENGTH_LONG).show()
            vm.clearAlerts()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Send Feedback") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Outlined.Feedback, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Type
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                TextField(
                    modifier = Modifier
                        .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true) // ⬅️ UPDATED
                        .fillMaxWidth(),
                    value = ui.type,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Feature") },
                        onClick = { expanded = false; vm.update { it.copy(type = "Feature") } }
                    )
                    DropdownMenuItem(
                        text = { Text("Bug") },
                        onClick = { expanded = false; vm.update { it.copy(type = "Bug") } }
                    )
                }
            }

            // Title
            OutlinedTextField(
                value = ui.title,
                onValueChange = { vm.update { s -> s.copy(title = it) } },
                label = { Text("Title *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            // Description
            OutlinedTextField(
                value = ui.description,
                onValueChange = { vm.update { s -> s.copy(description = it) } },
                label = { Text("Description *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                maxLines = 6
            )

            if (ui.type == "Bug") {
                // Severity
                var sevExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = sevExpanded, onExpandedChange = { sevExpanded = it }) {
                    TextField(
                        modifier = Modifier
                            .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true) // ⬅️ UPDATED
                            .fillMaxWidth(),
                        value = ui.severity,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Severity") },
                        leadingIcon = { Icon(Icons.Outlined.BugReport, null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sevExpanded) }
                    )
                    ExposedDropdownMenu(expanded = sevExpanded, onDismissRequest = { sevExpanded = false }) {
                        listOf("Low","Med","High","Critical").forEach { level ->
                            DropdownMenuItem(text = { Text(level) }, onClick = {
                                sevExpanded = false; vm.update { s -> s.copy(severity = level) }
                            })
                        }
                    }
                }

                // Steps to reproduce
                OutlinedTextField(
                    value = ui.stepsToReproduce,
                    onValueChange = { vm.update { s -> s.copy(stepsToReproduce = it) } },
                    label = { Text("Steps to reproduce") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    maxLines = 5
                )
            }

            // Contact email (optional)
            OutlinedTextField(
                value = ui.contactEmail,
                onValueChange = { vm.update { s -> s.copy(contactEmail = it) } },
                label = { Text("Reply-to email (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )

            // Screenshot URL (optional)
            OutlinedTextField(
                value = ui.screenshotUrl,
                onValueChange = { vm.update { s -> s.copy(screenshotUrl = it) } },
                label = { Text("Screenshot URL (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                )
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { vm.submit() },
                enabled = !ui.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (ui.isSubmitting) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Submitting…")
                } else {
                    Text(if (ui.type == "Bug") "Submit Bug" else "Submit Feature")
                }
            }

            Spacer(Modifier.height(8.dp))
            ProvideTextStyle(MaterialTheme.typography.bodySmall) {
                Text("We’ll only use your email to follow up if needed.")
            }
        }
    }
}
