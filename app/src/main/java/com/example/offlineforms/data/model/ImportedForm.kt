package com.example.offlineforms.data.model

data class ImportedForm(
    val id: String = "",
    val title: String = "",
    val fields: List<FormField> = emptyList(),
    val importedAt: Long = System.currentTimeMillis(),
    val originalCreatorId: String = "",
    val originalFormId: String = ""
)