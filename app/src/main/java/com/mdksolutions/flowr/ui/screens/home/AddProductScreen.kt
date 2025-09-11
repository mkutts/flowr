package com.mdksolutions.flowr.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mdksolutions.flowr.model.Product
import com.mdksolutions.flowr.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(navController: NavController, viewModel: HomeViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }

    // --- State dropdown ---
    val stateOptions = listOf(
        "Alabama","Alaska","Arizona","Arkansas","California","Colorado","Connecticut","Delaware",
        "District of Columbia","Florida","Georgia","Hawaii","Idaho","Illinois","Indiana","Iowa",
        "Kansas","Kentucky","Louisiana","Maine","Maryland","Massachusetts","Michigan","Minnesota",
        "Mississippi","Missouri","Montana","Nebraska","Nevada","New Hampshire","New Jersey",
        "New Mexico","New York","North Carolina","North Dakota","Ohio","Oklahoma","Oregon",
        "Pennsylvania","Rhode Island","South Carolina","South Dakota","Tennessee","Texas","Utah",
        "Vermont","Virginia","Washington","West Virginia","Wisconsin","Wyoming"
    )
    var selectedState by remember { mutableStateOf("") }
    var stateMenuExpanded by remember { mutableStateOf(false) }

    // --- Strain Type dropdown (existing) ---
    val strainOptions = listOf("Indica", "Sativa", "Hybrid", "Sativa-hybrid", "Indica-Hybrid")
    var strainType by remember { mutableStateOf("") }
    var strainMenuExpanded by remember { mutableStateOf(false) }

    var validationMessage by remember { mutableStateOf<String?>(null) }

    // ✅ Show Snackbar for ViewModel messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            if (it.contains("success", ignoreCase = true)) {
                navController.popBackStack()
            }
            viewModel.clearMessage()
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
        topBar = { TopAppBar(title = { Text("Add Product") }) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Add Product", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Brand") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (flower, edible, etc.)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // 🆕 State dropdown (replaces free-text)
                ExposedDropdownMenuBox(
                    expanded = stateMenuExpanded,
                    onExpandedChange = { stateMenuExpanded = !stateMenuExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedState,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("State") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = stateMenuExpanded,
                        onDismissRequest = { stateMenuExpanded = false }
                    ) {
                        stateOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedState = option
                                    stateMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 🆕 Strain Type dropdown
                ExposedDropdownMenuBox(
                    expanded = strainMenuExpanded,
                    onExpandedChange = { strainMenuExpanded = !strainMenuExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = strainType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Strain Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = strainMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = strainMenuExpanded,
                        onDismissRequest = { strainMenuExpanded = false }
                    ) {
                        strainOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    strainType = option
                                    strainMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = {
                            if (name.isNotEmpty() &&
                                brand.isNotEmpty() &&
                                category.isNotEmpty() &&
                                selectedState.isNotEmpty() &&
                                strainType.isNotEmpty()
                            ) {
                                val product = Product(
                                    name = name,
                                    brand = brand,
                                    category = category,
                                    state = selectedState, // 👈 use dropdown value
                                    strainType = strainType
                                )
                                viewModel.addProduct(product)
                            } else {
                                validationMessage = "Please fill in all fields (including State and Strain Type)"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Product")
                    }
                }
            }
        }
    }
}
