package com.example.offlineforms.ui.screens

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.offlineforms.data.model.FieldType
import com.example.offlineforms.data.model.FormField
import com.example.offlineforms.ui.viewmodel.FormViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormPreviewScreen(
    navController: NavController,
    formId: String,
    formViewModel: FormViewModel = viewModel()
) {
    val currentForm by formViewModel.currentForm.collectAsState()
    val isLoading by formViewModel.isLoading.collectAsState()

    // Load form if coming from home screen edit flow
    LaunchedEffect(formId) {
        if (formId != "preview_temp") {
            formViewModel.loadFormById(formId)
        }
        // If "preview_temp" we already have it in currentForm
        // from the builder screen's setCurrentForm() call
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Preview",
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back to builder"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Back to builder", fontSize = 16.sp)
                }
            }
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            currentForm == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Form not found.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    // Form title
                    item {
                        Text(
                            text = currentForm!!.title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    item {
                        Text(
                            text = "This is a preview. Fields are not fillable here.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                    }

                    item { Divider() }

                    // Render each field in preview mode
                    items(items = currentForm!!.fields) { field: FormField ->
                        PreviewFieldItem(field = field)
                    }
                }
            }
        }
    }
}

// Renders each field as it would look when filled
// All inputs are disabled since this is preview only
@Composable
fun PreviewFieldItem(field: FormField) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Question label with required indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = field.label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (field.isRequired) {
                Text(
                    text = "*",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Render different UI based on field type
        when (field.type) {
            FieldType.TEXT -> {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    placeholder = { Text("Enter text…") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    shape = RoundedCornerShape(8.dp)
                )
            }

            FieldType.NUMBER -> {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    placeholder = { Text("Enter number…") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    shape = RoundedCornerShape(8.dp)
                )
            }

            FieldType.DATE -> {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    placeholder = { Text("DD / MM / YYYY") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    shape = RoundedCornerShape(8.dp),
                    trailingIcon = {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null
                        )
                    }
                )
            }

            FieldType.DROPDOWN -> {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    placeholder = { Text("Select…") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    shape = RoundedCornerShape(8.dp),
                    trailingIcon = {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }
                )
                if (field.options.isNotEmpty()) {
                    Text(
                        text = "Options: ${field.options.joinToString(", ")}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                }
            }

            FieldType.CHECKBOX -> {
                field.options.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = false,
                            onCheckedChange = {},
                            enabled = false
                        )
                        Text(
                            text = option,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            FieldType.RADIO -> {
                field.options.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = false,
                            onClick = {},
                            enabled = false
                        )
                        Text(
                            text = option,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }
}