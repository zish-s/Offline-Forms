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
import androidx.navigation.NavController
import com.example.offlineforms.data.model.FormField
import com.example.offlineforms.data.model.FormSubmission
import com.example.offlineforms.Navigation.Routes
import com.example.offlineforms.ui.viewmodel.FormViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FillImportedFormScreen(
    navController: NavController,
    importId: String,
    formViewModel: FormViewModel
) {
    val importedForms by formViewModel.importedForms.collectAsState()
    val importedForm = importedForms.find { it.id == importId }
    val isLoading by formViewModel.isLoading.collectAsState()

    var answers by remember { mutableStateOf(mapOf<String, String>()) }

    LaunchedEffect(Unit) {
        formViewModel.loadImportedForms()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = importedForm?.title ?: "Fill form",
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
                Button(
                    onClick = {
                        importedForm?.let { form ->
                            val missingRequired = form.fields
                                .filter { it.isRequired }
                                .any { field -> answers[field.id].isNullOrEmpty() }

                            if (!missingRequired) {
                                val submission = FormSubmission(
                                    formId = form.originalFormId
                                        .ifEmpty { form.id },
                                    formTitle = form.title,
                                    answers = answers
                                )
                                formViewModel.saveSubmission(submission) {
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(Routes.HOME) { inclusive = false }
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading && importedForm != null
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
    ) { paddingValues ->
        when {
            importedForm == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
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
                            text = importedForm.title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    item { HorizontalDivider() }

                    items(importedForm.fields) { field: FormField ->
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