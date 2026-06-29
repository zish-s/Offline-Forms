package com.example.offlineforms.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.offlineforms.data.model.Form
import com.example.offlineforms.data.model.FormField
import com.example.offlineforms.Navigation.Routes
import com.example.offlineforms.ui.viewmodel.FormViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormBuilderScreen(
    navController: NavController,
    formId: String?,
    formViewModel: FormViewModel = viewModel()
) {
    // Local UI state for the form being built
    var formTitle by remember { mutableStateOf("") }
    var fields by remember { mutableStateOf(listOf<FormField>()) }

    // Local state for the question being added
    var questionLabel by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(FieldType.TEXT) }
    var isRequired by remember { mutableStateOf(false) }
    var optionsInput by remember { mutableStateOf("") }

    val isLoading by formViewModel.isLoading.collectAsState()
    val currentForm by formViewModel.currentForm.collectAsState()

    // If editing an existing form, load it and populate fields
    LaunchedEffect(formId) {
        if (formId != null) {
            formViewModel.loadFormById(formId)
        } else {
            formViewModel.clearCurrentForm()
        }
    }

    // When currentForm loads, populate local state
    LaunchedEffect(currentForm) {
        currentForm?.let { form ->
            formTitle = form.title
            fields = form.fields
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (formId != null) "Edit form" else "New form",
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Preview button - only show if there's at least a title
                    if (formTitle.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                // Save temporarily then navigate to preview
                                val tempForm = Form(
                                    id = formId ?: "",
                                    title = formTitle,
                                    fields = fields
                                )
                                formViewModel.setCurrentForm(tempForm)
                                navController.navigate(
                                    if (formId != null)
                                        "form_preview/$formId"
                                    else
                                        "form_preview/preview_temp"
                                )
                            }
                        ) {
                            Text(
                                text = "Preview",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Save button at bottom
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Button(
                    onClick = {
                        val form = Form(
                            id = formId ?: "",
                            title = formTitle,
                            fields = fields
                        )
                        formViewModel.saveForm(form) {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.HOME) { inclusive = false }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading && formTitle.isNotEmpty()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save form", fontSize = 16.sp)
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Form title input
            item {
                OutlinedTextField(
                    value = formTitle,
                    onValueChange = { formTitle = it },
                    label = { Text("Form title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp
                    )
                )
            }

            // Added questions list
            if (fields.isNotEmpty()) {
                item {
                    Text(
                        text = "Questions (${fields.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                items(fields) { field ->
                    FieldCard(
                        field = field,
                        onDelete = {
                            fields = fields.filter { it.id != field.id }
                        }
                    )
                }
            }

            // Divider between questions and add section
            item {
                Divider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
            }

            // Add question section
            item {
                Text(
                    text = "Add question",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Field type selector chips
            item {
                FieldTypeSelector(
                    selectedType = selectedType,
                    onTypeSelected = {
                        selectedType = it
                        optionsInput = ""
                    }
                )
            }

            // Question label input
            item {
                OutlinedTextField(
                    value = questionLabel,
                    onValueChange = { questionLabel = it },
                    label = { Text("Question label") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Options input - only for dropdown, checkbox, radio
            if (selectedType in listOf(
                    FieldType.DROPDOWN,
                    FieldType.CHECKBOX,
                    FieldType.RADIO
                )
            ) {
                item {
                    OutlinedTextField(
                        value = optionsInput,
                        onValueChange = { optionsInput = it },
                        label = { Text("Options (comma separated)") },
                        placeholder = { Text("e.g. Yes, No, Maybe") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            // Required toggle
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = isRequired,
                        onCheckedChange = { isRequired = it }
                    )
                    Text(
                        text = "Required",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Add question button
            item {
                Button(
                    onClick = {
                        if (questionLabel.isNotEmpty()) {
                            val newField = FormField(
                                id = UUID.randomUUID().toString(),
                                label = questionLabel,
                                type = selectedType,
                                isRequired = isRequired,
                                options = if (optionsInput.isNotEmpty())
                                    optionsInput.split(",").map { it.trim() }
                                else emptyList()
                            )
                            fields = fields + newField
                            // Reset input fields
                            questionLabel = ""
                            isRequired = false
                            optionsInput = ""
                            selectedType = FieldType.TEXT
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = questionLabel.isNotEmpty()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add question")
                }
            }
        }
    }
}

// Shows each added question as a card with a delete button
@Composable
fun FieldCard(
    field: FormField,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = field.label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (field.isRequired) {
                        Text(
                            text = "•",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 16.sp
                        )
                    }
                }
                Text(
                    text = field.type.name.lowercase()
                        .replaceFirstChar { it.uppercase() },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                if (field.options.isNotEmpty()) {
                    Text(
                        text = field.options.joinToString(", "),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove question",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// Scrollable row of field type chips
@Composable
fun FieldTypeSelector(
    selectedType: FieldType,
    onTypeSelected: (FieldType) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        FieldType.values().forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                label = {
                    Text(
                        text = type.name.lowercase()
                            .replaceFirstChar { it.uppercase() },
                        fontSize = 12.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}