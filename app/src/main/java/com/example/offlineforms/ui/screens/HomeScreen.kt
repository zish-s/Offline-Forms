package com.example.offlineforms.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.offlineforms.data.model.Form
import com.example.offlineforms.Navigation.Routes
import com.example.offlineforms.ui.viewmodel.FormViewModel
import com.example.offlineforms.utils.formatTimestamp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    formViewModel: FormViewModel = viewModel()
) {
    val forms by formViewModel.forms.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Load forms when screen first appears
    LaunchedEffect(Unit) {
        formViewModel.loadForms()
    }

    // Sidebar drawer wrapping the entire screen
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppSidebar(
                isAnonymous = formViewModel.isUserAnonymous(),
                onHomeClick = { scope.launch { drawerState.close() } },
                onFormsClick = { scope.launch { drawerState.close() } },
                onImportsClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Routes.IMPORTS)
                },
                onSignInClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Routes.LOGIN)
                },
                onSignOutClick = {
                    formViewModel.signOut {
                        navController.navigate(Routes.STARTUP) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "My forms",
                            fontWeight = FontWeight.Medium
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch { drawerState.open() }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open menu"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        navController.navigate(Routes.FORM_BUILDER)
                    },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create new form",
                        tint = Color.White
                    )
                }
            }
        ) { paddingValues ->
            if (forms.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Article,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                        )
                        Text(
                            text = "No forms yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Tap + to create your first form.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                // Forms list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(forms) { form ->
                        FormCard(
                            form = form,
                            onEditClick = {
                                navController.navigate("form_builder/${form.id}")
                            },
                            onFillClick = {
                                navController.navigate("fill_form/${form.id}")
                            },
                            onResponsesClick = {
                                navController.navigate("responses/${form.id}")
                            },
                            onDeleteClick = {
                                formViewModel.deleteForm(form.id)
                            },
                            onShareClick = {
                                val jsonString = formViewModel.exportFormAsJson(form)
                                val fileName = "${form.title.replace(" ", "_")}_form.json"

                                // Write to cache directory
                                val file = java.io.File(context.cacheDir, fileName)
                                file.writeText(jsonString)

                                // Get secure URI via FileProvider
                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    file
                                )

                                // Open Android share sheet
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "OfflineForms: ${form.title}")
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    android.content.Intent.createChooser(shareIntent, "Share form via")
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun FormCard(
    form: Form,
    onEditClick: () -> Unit,
    onFillClick: () -> Unit,
    onResponsesClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete form") },
            text = { Text("Are you sure you want to delete \"${form.title}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClick()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Form title
            Text(
                text = form.title.ifEmpty { "Untitled form" },
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Question count and last edited
            Text(
                text = "${form.fields.size} question${if (form.fields.size != 1) "s" else ""} · edited ${
                    formatTimestamp(form.updatedAt)
                }",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            // Sync status badge
            SyncBadge(isSynced = form.isSynced)

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )

            // Action buttons row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                //share button

                OutlinedButton(
                    onClick = onShareClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Share", fontSize = 12.sp)
                }

                // Edit button
                OutlinedButton(
                    onClick = onEditClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Edit", fontSize = 12.sp)
                }

                // Fill button
                OutlinedButton(
                    onClick = onFillClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.EditNote,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Fill", fontSize = 12.sp)
                }

                // Responses button
                OutlinedButton(
                    onClick = onResponsesClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Inbox,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Responses", fontSize = 12.sp)
                }

                // Delete button
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete form",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SyncBadge(isSynced: Boolean) {
    val backgroundColor = if (isSynced)
        Color(0xFFE1F5EE) else Color(0xFFFDF3DC)
    val textColor = if (isSynced)
        Color(0xFF085041) else Color(0xFF854F0B)
    val text = if (isSynced) "Synced to cloud" else "Saved locally"
    val icon = if (isSynced) Icons.Default.CloudDone else Icons.Default.SaveAlt

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = text,
                fontSize = 11.sp,
                color = textColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun AppSidebar(
    isAnonymous: Boolean,
    onHomeClick: () -> Unit,
    onFormsClick: () -> Unit,
    onImportsClick: () -> Unit,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit

) {
    ModalDrawerSheet(
        drawerContainerColor = Color(0xFF26215C)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "OfflineForms",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        NavigationDrawerItem(
            label = { Text("Home", color = Color.White) },
            selected = true,
            onClick = onHomeClick,
            icon = {
                Icon(Icons.Default.Home, contentDescription = null, tint = Color.White)
            },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = Color(0xFF534AB7),
                unselectedContainerColor = Color.Transparent
            )
        )

        NavigationDrawerItem(
            label = { Text("Imports", color = Color.White) },
            selected = false,
            onClick = onImportsClick,
            icon = {
                Icon(
                    Icons.Default.Download,
                    contentDescription = null,
                    tint = Color(0xFFAFA9EC)
                )
            },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = Color(0xFF534AB7),
                unselectedContainerColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.weight(1f))

        // Show different option based on auth state
        if (isAnonymous) {
            NavigationDrawerItem(
                label = { Text("Sign in to sync", color = Color(0xFFAFA9EC)) },
                selected = false,
                onClick = onSignInClick,
                icon = {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = Color(0xFFAFA9EC)
                    )
                },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = Color(0xFF534AB7),
                    unselectedContainerColor = Color.Transparent
                )
            )
        } else {
            NavigationDrawerItem(
                label = { Text("Sign out", color = Color(0xFFAFA9EC)) },
                selected = false,
                onClick = onSignOutClick,
                icon = {
                    Icon(
                        Icons.Default.Logout,
                        contentDescription = null,
                        tint = Color(0xFFAFA9EC)
                    )
                },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = Color(0xFF534AB7),
                    unselectedContainerColor = Color.Transparent
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
