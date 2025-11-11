package com.mdksolutions.flowr.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mdksolutions.flowr.model.Product
import com.mdksolutions.flowr.viewmodel.HomeViewModel
import java.util.Locale
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults

// Lookup VM + autocomplete
import com.mdksolutions.flowr.viewmodel.LookupViewModel
import com.mdksolutions.flowr.ui.components.AutoCompleteTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel(),
    lookupViewModel: LookupViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // ---- Local screen state ----
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var strain by remember { mutableStateOf("") } // mirrors `name`

    // --- Category dropdown ---
    val categoryOptions = listOf("Flower", "Edible", "Vape", "Concentrate", "Pre-Roll", "Other")
    var category by remember { mutableStateOf("") }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var otherCategory by remember { mutableStateOf("") } // shown when category == "Other"

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

    // --- Strain Type dropdown ---
    val strainOptions = listOf("Indica", "Sativa", "Hybrid", "Sativa-hybrid", "Indica-Hybrid")
    var strainType by remember { mutableStateOf("") }
    var strainMenuExpanded by remember { mutableStateOf(false) }

    // Potency toggle + adaptive input
    var potencyUsesMg by remember { mutableStateOf(false) } // false = %, true = mg
    var potencyText by remember { mutableStateOf("") }

    // CBD mirrors THC unit mode
    var cbdText by remember { mutableStateOf("") }

    LaunchedEffect(category) {
        val cat = (if (category == "Other") otherCategory else category).lowercase(Locale.US)
        potencyUsesMg = cat.contains("edible") || cat.contains("drink")
    }

    var validationMessage by remember { mutableStateOf<String?>(null) }

    // VM messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            if (it.contains("success", ignoreCase = true)) {
                navController.popBackStack()
            }
            viewModel.clearMessage()
        }
    }

    // validation messages
    LaunchedEffect(validationMessage) {
        validationMessage?.let {
            snackbarHostState.showSnackbar(it)
            validationMessage = null
        }
    }

    // ----- SCROLL + INSETS FIX -----
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Lookup UI state
    val lookupUi = lookupViewModel.ui.collectAsState().value

    // Keep local fields synced with VM query (brand/strain only)
    LaunchedEffect(lookupUi.brandQuery) {
        if (lookupUi.brandQuery != brand) brand = lookupUi.brandQuery
    }
    LaunchedEffect(lookupUi.strainQuery) {
        if (lookupUi.strainQuery != strain) {
            strain = lookupUi.strainQuery
            name = lookupUi.strainQuery // mirror into Product name
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Product") },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
            ) {
                // Product / Strain (drives Product.name)
                item {
                    AutoCompleteTextField(
                        label = "Product / Strain",
                        value = strain,
                        suggestions = lookupUi.strainSuggestions,
                        onValueChange = {
                            strain = it
                            name = it
                            lookupViewModel.onStrainQueryChange(it)
                        },
                        onItemSelected = { selected ->
                            strain = selected
                            name = selected
                            lookupViewModel.onStrainQueryChange(selected)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Brand
                item {
                    AutoCompleteTextField(
                        label = "Brand",
                        value = brand,
                        suggestions = lookupUi.brandSuggestions,
                        onValueChange = {
                            brand = it
                            lookupViewModel.onBrandQueryChange(it)
                        },
                        onItemSelected = { selected ->
                            brand = selected
                            lookupViewModel.onBrandQueryChange(selected)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Category
                item {
                    ExposedDropdownMenuBox(
                        expanded = categoryMenuExpanded,
                        onExpandedChange = { categoryMenuExpanded = !categoryMenuExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = category.ifEmpty { "" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryMenuExpanded,
                            onDismissRequest = { categoryMenuExpanded = false }
                        ) {
                            categoryOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        category = option
                                        if (option != "Other") otherCategory = ""
                                        categoryMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Custom category if "Other"
                item {
                    if (category == "Other") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = otherCategory,
                            onValueChange = { otherCategory = it },
                            label = { Text("Custom Category") },
                            placeholder = { Text("e.g., Tincture, Topical, Capsule…") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // State
                item {
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
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
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
                }

                // Strain type
                item {
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
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
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
                }

                // Potency mode + inputs (THC + CBD)
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Potency mode", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = !potencyUsesMg,
                            onClick = { potencyUsesMg = false },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) { Text("Percentage (%)") }

                        SegmentedButton(
                            selected = potencyUsesMg,
                            onClick = { potencyUsesMg = true },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) { Text("Dosage (mg)") }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Applies to both THC and CBD.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))

                    // THC
                    OutlinedTextField(
                        value = potencyText,
                        onValueChange = { input ->
                            potencyText = input.replace("[^0-9.]".toRegex(), "")
                        },
                        label = { Text(if (potencyUsesMg) "Dosage (mg) – Optional" else "THC % – Optional") },
                        placeholder = { Text(if (potencyUsesMg) "e.g. 10" else "e.g. 18.5") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // CBD mirrors THC unit mode
                    OutlinedTextField(
                        value = cbdText,
                        onValueChange = { input ->
                            cbdText = input.replace("[^0-9.]".toRegex(), "")
                        },
                        label = { Text(if (potencyUsesMg) "CBD Dosage (mg) – Optional" else "CBD % – Optional") },
                        placeholder = { Text(if (potencyUsesMg) "e.g. 10" else "e.g. 0.5") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Submit
                item {
                    if (uiState.isLoading) {
                        CircularProgressIndicator()
                    } else {
                        Button(
                            onClick = {
                                val finalCategory = if (category == "Other") otherCategory.trim() else category
                                val categoryValid = finalCategory.isNotEmpty()

                                val cleanName  = name.trim().replace("\\s+".toRegex(), " ")
                                val cleanBrand = brand.trim().replace("\\s+".toRegex(), " ")

                                if (cleanName.isNotEmpty() &&
                                    cleanBrand.isNotEmpty() &&
                                    categoryValid &&
                                    selectedState.isNotEmpty() &&
                                    strainType.isNotEmpty()
                                ) {
                                    val parsedThc = potencyText.toDoubleOrNull()
                                    val safeThc = when {
                                        parsedThc == null -> null
                                        potencyUsesMg -> parsedThc.coerceIn(0.0, 10000.0)
                                        else -> parsedThc.coerceIn(0.0, 100.0)
                                    }

                                    val parsedCbd = cbdText.toDoubleOrNull()
                                    val safeCbd = when {
                                        parsedCbd == null -> null
                                        potencyUsesMg -> parsedCbd.coerceIn(0.0, 10000.0)
                                        else -> parsedCbd.coerceIn(0.0, 100.0)
                                    }

                                    val product = Product(
                                        name = cleanName,
                                        brand = cleanBrand,
                                        category = finalCategory,
                                        state = selectedState,
                                        strainType = strainType,
                                        potencyUsesMg = potencyUsesMg,
                                        thcPercent = if (!potencyUsesMg) safeThc else null,
                                        dosageMg   = if (potencyUsesMg)  safeThc else null,
                                        cbdPercent = if (!potencyUsesMg) safeCbd else null,
                                        cbdMg      = if (potencyUsesMg)  safeCbd else null
                                    )
                                    viewModel.addProductUnique(product)
                                } else {
                                    validationMessage = when {
                                        cleanName.isEmpty() -> "Please enter a product/strain name"
                                        cleanBrand.isEmpty() -> "Please enter a brand"
                                        category.isEmpty() -> "Please select a category"
                                        category == "Other" && otherCategory.isBlank() -> "Please enter a custom category"
                                        selectedState.isEmpty() -> "Please select a state"
                                        strainType.isEmpty() -> "Please select a strain type"
                                        else -> "Please fill in all fields"
                                    }
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
}
