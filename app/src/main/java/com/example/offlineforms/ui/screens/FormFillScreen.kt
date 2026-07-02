package com.example.offlineforms.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.offlineforms.data.model.FieldType
import com.example.offlineforms.data.model.FormField
import com.example.offlineforms.data.model.FormSubmission
import com.example.offlineforms.Navigation.Routes
import com.example.offlineforms.ui.viewmodel.FormViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FillFormScreen(
    navController: NavController,
    formId: String,
    formViewModel: FormViewModel = viewModel()
) {
    val currentForm by formViewModel.currentForm.collectAsState()
    val isLoading by formViewModel.isLoading.collectAsState()
    val errorMessage by formViewModel.errorMessage.collectAsState()

    // answers map: fieldId -> user's answer as string
    var answers by remember { mutableStateOf(mapOf<String, String>()) }

    LaunchedEffect(formId) {
        formViewModel.loadFormById(formId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentForm?.title ?: "Fill form",
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                Column(modifier = Modifier.padding(16.dp)) {
                    // Show error if any
                    errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LaunchedEffect(error) {
                            formViewModel.clearError()
                        }
                    }

                    Button(
                        onClick = {
                            currentForm?.let { form ->
                                // Check required fields are filled
                                val missingRequired = form.fields
                                    .filter { it.isRequired }
                                    .any { field ->
                                        answers[field.id].isNullOrEmpty()
                                    }

                                if (!missingRequired) {
                                    val submission = FormSubmission(
                                        formId = form.id,
                                        formTitle = form.title,
                                        answers = answers
                                    )
                                    formViewModel.saveSubmission(submission) {
                                        navController.navigate(Routes.HOME) {
                                            popUpTo(Routes.HOME) { inclusive = false }
                                        }
                                    }
                                } else {
                                    formViewModel.clearError()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading && currentForm != null
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Submit response", fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        when {
            isLoading && currentForm == null -> {
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
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    item {
                        Text(
                            text = currentForm!!.title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    item { HorizontalDivider() }

                    items(currentForm!!.fields) { field: FormField ->
                        FillFieldItem(
                            field = field,
                            currentAnswer = answers[field.id] ?: "",
                            onAnswerChange = { newAnswer ->
                                answers = answers + (field.id to newAnswer)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FillFieldItem(
    field: FormField,
    currentAnswer: String,
    onAnswerChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Question label
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

        // Active input based on field type
        when (field.type) {
            FieldType.TEXT -> {
                OutlinedTextField(
                    value = currentAnswer,
                    onValueChange = onAnswerChange,
                    placeholder = { Text("Enter text…") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
            }

            FieldType.NUMBER -> {
                OutlinedTextField(
                    value = currentAnswer,
                    onValueChange = { value ->
                        if (value.all { it.isDigit() || it == '.' }) {
                            onAnswerChange(value)
                        }
                    },
                    placeholder = { Text("Enter number…") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true
                )
            }

            FieldType.DATE -> {
                OutlinedTextField(
                    value = currentAnswer,
                    onValueChange = onAnswerChange,
                    placeholder = { Text("DD / MM / YYYY") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    trailingIcon = {
                        Icon(Icons.Default.CalendarToday, contentDescription = null)
                    },
                    singleLine = true
                )
            }

            FieldType.DROPDOWN -> {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = currentAnswer.ifEmpty { "Select…" },
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(8.dp),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        field.options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onAnswerChange(option)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            FieldType.CHECKBOX -> {
                // For checkboxes we store comma-separated selected values
                val selectedOptions = if (currentAnswer.isEmpty())
                    emptyList()
                else
                    currentAnswer.split(",")

                field.options.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = option in selectedOptions,
                            onCheckedChange = { checked ->
                                val updated = if (checked) {
                                    selectedOptions + option
                                } else {
                                    selectedOptions - option
                                }
                                onAnswerChange(updated.joinToString(","))
                            }
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
                            selected = currentAnswer == option,
                            onClick = { onAnswerChange(option) }
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