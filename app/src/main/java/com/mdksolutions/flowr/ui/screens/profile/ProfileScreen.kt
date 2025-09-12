package com.mdksolutions.flowr.ui.screens.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, viewModel: ProfileViewModel = viewModel()) {
    val ui by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.signOut()
                        navController.navigate("auth") { popUpTo(0) } // go to login
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Sign out")
                    }
                }
            )
        }
    ) { padding ->
        when {
            ui.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            ui.error != null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(ui.error ?: "Error", color = MaterialTheme.colorScheme.error)
                }
            }
            else -> {
                val p = ui.profile
                if (p == null) {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Text("Not signed in")
                    }
                } else {
                    ProfileContent(
                        displayName = p.displayName,
                        email = p.email,
                        photoUrl = p.photoUrl,
                        role = p.role,
                        reviewCount = ui.reviewCount,
                        onRename = { viewModel.updateDisplayName(it) },
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
    email: String,
    photoUrl: String?,
    role: String,
    reviewCount: Int,
    onRename: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var editing by remember { mutableStateOf(false) }
    var nameDraft by remember { mutableStateOf(displayName) }

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
                Text(text = email, style = MaterialTheme.typography.bodyMedium)
                if (role.isNotBlank()) {
                    Text(text = "Role: $role", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        HorizontalDivider()

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AssistChip(onClick = {}, label = { Text("Reviews: $reviewCount") })
        }

        HorizontalDivider()

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
                OutlinedButton(onClick = { editing = false; nameDraft = displayName }) {
                    Text("Cancel")
                }
            }
        } else {
            OutlinedButton(onClick = { editing = true }) { Text("Edit Profile") }
        }
    }
}
