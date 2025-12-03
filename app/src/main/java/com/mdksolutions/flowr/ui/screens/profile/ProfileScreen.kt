package com.mdksolutions.flowr.ui.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.mdksolutions.flowr.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, viewModel: ProfileViewModel = viewModel()) {

    val ui by viewModel.uiState.collectAsState()

    // 🔧 DEBUG LOG — helps confirm the correct count is reaching UI
    LaunchedEffect(ui.reviewCount) {
        android.util.Log.d("ProfileScreen", "ReviewCount from VM = ${ui.reviewCount}")
    }

    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.uploadProfilePhoto(uri)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.signOut()
                        navController.navigate("auth") { popUpTo(0) }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign out")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->

        when {
            ui.isLoading ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

            ui.error != null ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) { Text(ui.error ?: "Error", color = MaterialTheme.colorScheme.error) }

            else -> {
                val p = ui.profile
                if (p == null) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) { Text("Not signed in") }
                } else {
                    ProfileContent(
                        displayName = p.displayName,
                        username = p.username,
                        photoUrl = p.photoUrl,
                        role = p.role,

                        // ✅ PATCH: always use ui.reviewCount (NEVER profile.reviewCount)
                        reviewCount = ui.reviewCount,

                        isUploading = ui.isUploading,
                        onChangePhoto = {
                            pickPhotoLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onOpenReviews = { navController.navigate("my_reviews") },
                        onOpenFollowing = { navController.navigate("following") },
                        onOpenWork = { navController.navigate("edit_work") },
                        onRename = { viewModel.updateDisplayName(it) },
                        onChangeUsername = { newU ->
                            viewModel.updateUsername(newU) { success, msg ->
                                val feedback =
                                    msg ?: if (success) "Username updated!" else "Update failed."
                                scope.launch { snackbarHostState.showSnackbar(feedback) }
                            }
                        },
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(
    displayName: String,
    username: String,
    photoUrl: String?,
    role: String,

    // Already patched value
    reviewCount: Int,

    isUploading: Boolean,
    onChangePhoto: () -> Unit,
    onOpenReviews: () -> Unit,
    onOpenFollowing: () -> Unit,
    onOpenWork: () -> Unit,
    onRename: (String) -> Unit,
    onChangeUsername: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var editing by remember { mutableStateOf(false) }
    var nameDraft by remember { mutableStateOf(displayName) }

    var showUsernameDialog by remember { mutableStateOf(false) }
    var usernameDraft by remember { mutableStateOf(username) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val painter = rememberAsyncImagePainter(model = photoUrl)
            Surface(
                modifier = Modifier.size(72.dp).clip(CircleShape),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                if (photoUrl != null) {
                    Image(
                        painter = painter,
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName.ifBlank { "Unnamed" },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "@" + username.ifBlank { "unknown" },
                    style = MaterialTheme.typography.bodyMedium
                )
                if (role.isNotBlank()) {
                    Text(text = "Role: $role", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (isUploading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        OutlinedButton(onClick = onChangePhoto) { Text("Change photo") }

        HorizontalDivider()

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {

            // ⭐ GUARANTEED PATCHED LINE
            AssistChip(onClick = onOpenReviews, label = { Text("Reviews: $reviewCount") })

            AssistChip(onClick = onOpenFollowing, label = { Text("Following") })

            if (role.equals("budtender", ignoreCase = true)) {
                AssistChip(onClick = onOpenWork, label = { Text("Work schedule") })
            }
        }

        HorizontalDivider()

        // Display name edit section
        if (editing) {
            OutlinedTextField(
                value = nameDraft,
                onValueChange = { nameDraft = it },
                label = { Text("Display name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    onRename(nameDraft.trim())
                    editing = false
                }) { Text("Save") }
                OutlinedButton(onClick = {
                    editing = false
                    nameDraft = displayName
                }) { Text("Cancel") }
            }
        } else {
            OutlinedButton(onClick = { editing = true }) { Text("Edit Profile") }
        }

        // Username block
        Text("Username", style = MaterialTheme.typography.labelLarge)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "@" + username.ifBlank { "unknown" },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            TextButton(onClick = {
                usernameDraft = username
                showUsernameDialog = true
            }) { Text("Change") }
        }

        if (showUsernameDialog) {
            AlertDialog(
                onDismissRequest = { showUsernameDialog = false },
                title = { Text("Change username") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = usernameDraft,
                            onValueChange = {
                                val filtered =
                                    it.filter { ch -> ch.isLetterOrDigit() || ch == '_' || ch == '.' }
                                usernameDraft = filtered
                            },
                            label = { Text("New username") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "3–24 characters; letters, numbers, _ or .; not case-sensitive.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val u = usernameDraft.trim()
                        if (u.length < 3 || u.length > 24 || u.startsWith(".") || u.endsWith(".")) {
                            return@TextButton
                        }
                        onChangeUsername(u)
                        showUsernameDialog = false
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showUsernameDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
