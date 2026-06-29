package com.example.offlineforms.data.model


//It translates your text boxes into an organized data format that Firestore understands.
// It tells the app exactly what a completed form looks like (e.g., Name: String, Age: Int, Timestamp: Long).


//A Form is the template.
// A FormField is one question inside that form.
// A FormSubmission is one person's filled response.


data class FormSubmission(
    val id: String = "",
    val formId: String = "",
    val formTitle: String = "",
    val answers: Map<String, String> = emptyMap(),
    val submittedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val userId: String = ""
)