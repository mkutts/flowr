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

// ⬇️ NEW: imports for lookup VM + autocomplete
import com.mdksolutions.flowr.viewmodel.LookupViewModel
import com.mdksolutions.flowr.ui.components.AutoCompleteTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel(),
    // ⬇️ NEW: second VM for lookups (Firestore-backed)
    lookupViewModel: LookupViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // ---- Local screen state ----
    // We’ll treat "name" as the actual Product Name stored to Firestore,
    // but it’s now driven by the Strain autocomplete below.
    var name by remember { mutableStateOf("") }

    // Brand/Strain/Other are user inputs that drive autocomplete & saving.
    var brand by remember { mutableStateOf("") }
    var strain by remember { mutableStateOf("") } // mirrors `name`
    var other by remember { mutableStateOf("") }  // optional; not yet saved to Product model

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

    // ⬇️ NEW: CBD input mirrors THC input and toggle
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

    // ⬇️ NEW: collect lookup UI state
    val lookupUi = lookupViewModel.ui.collectAsState().value

    // ⬇️ NEW: keep local fields synced with VM query (so dropdowns reflect typing + selections)
    LaunchedEffect(lookupUi.brandQuery) {
        if (lookupUi.brandQuery != brand) brand = lookupUi.brandQuery
    }
    LaunchedEffect(lookupUi.strainQuery) {
        if (lookupUi.strainQuery != strain) {
            strain = lookupUi.strainQuery
            name = lookupUi.strainQuery // mirror into Product name
        }
    }
    LaunchedEffect(lookupUi.otherQuery) {
        if (lookupUi.otherQuery != other) other = lookupUi.otherQuery
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Product") },
                // navigationIcon = { /* optional back button */ },
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
                .imePadding()  // keyboard-safe
        ) {
            // Put the whole form into a scrollable LazyColumn
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
            ) {
                item {
                    Text("Add Product", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ⬇️ Product Name -> Strain autocomplete (mirrors to name)
                item {
                    AutoCompleteTextField(
                        label = "Product / Strain",
                        value = strain,
                        suggestions = lookupUi.strainSuggestions,
                        onValueChange = {
                            strain = it
                            name = it // keep Product name in sync
                            lookupViewModel.onStrainQueryChange(it)
                        },
                        onItemSelected = { selected ->
                            strain = selected
                            name = selected // keep Product name in sync
                            lookupViewModel.onStrainQueryChange(selected)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Brand autocomplete
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

                // Optional "Other" autocomplete (wired; not saved yet)
                item {
                    AutoCompleteTextField(
                        label = "Other (optional)",
                        value = other,
                        suggestions = lookupUi.otherSuggestions,
                        onValueChange = {
                            other = it
                            lookupViewModel.onOtherQueryChange(it)
                        },
                        onItemSelected = { selected ->
                            other = selected
                            lookupViewModel.onOtherQueryChange(selected)
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
                    Text(
                        "Applies to both THC and CBD.",
                        style = MaterialTheme.typography.bodySmall
                    )
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

                    // ⬇️ NEW: CBD mirrors THC unit mode
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

                                // 🔒 Normalize name/brand so keys match exactly (no dupes from spaces/case)
                                val cleanName  = name.trim().replace("\\s+".toRegex(), " ")
                                val cleanBrand = brand.trim().replace("\\s+".toRegex(), " ")

                                if (cleanName.isNotEmpty() &&
                                    cleanBrand.isNotEmpty() &&
                                    categoryValid &&
                                    selectedState.isNotEmpty() &&
                                    strainType.isNotEmpty()
                                ) {
                                    // THC parse
                                    val parsedThc = potencyText.toDoubleOrNull()
                                    val safeThc = when {
                                        parsedThc == null -> null
                                        potencyUsesMg -> parsedThc.coerceIn(0.0, 10000.0)
                                        else -> parsedThc.coerceIn(0.0, 100.0)
                                    }

                                    // ⬇️ NEW: CBD parse (same bounds)
                                    val parsedCbd = cbdText.toDoubleOrNull()
                                    val safeCbd = when {
                                        parsedCbd == null -> null
                                        potencyUsesMg -> parsedCbd.coerceIn(0.0, 10000.0)
                                        else -> parsedCbd.coerceIn(0.0, 100.0)
                                    }

                                    val product = Product(
                                        name = cleanName,              // from Strain autocomplete
                                        brand = cleanBrand,            // from Brand autocomplete
                                        category = finalCategory,
                                        state = selectedState,
                                        strainType = strainType,
                                        potencyUsesMg = potencyUsesMg,

                                        // THC
                                        thcPercent = if (!potencyUsesMg) safeThc else null,
                                        dosageMg   = if (potencyUsesMg)  safeThc else null,

                                        // ⬇️ NEW: CBD mirrors THC
                                        cbdPercent = if (!potencyUsesMg) safeCbd else null,
                                        cbdMg      = if (potencyUsesMg)  safeCbd else null
                                    )
                                    // ⬇️ ensure we use the duplicate-safe path
                                    viewModel.addProductUnique(product)

                                    // NOTE: `other` is captured above but not yet part of Product.
                                    // When you add it to the model, pass it here as well.
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
