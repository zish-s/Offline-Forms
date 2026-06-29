package com.example.offlineforms.data.model

//A Form is the template.
// A FormField is one question inside that form.


data class FormField(
    val id: String = "",
    val label: String = "",                    //the question text, e.g. "Inspector name"
    val type: FieldType = FieldType.TEXT,      // what kind of input it is.
    val isRequired: Boolean = false,
    val options: List<String> = emptyList()
)

enum class FieldType {
    TEXT,
    NUMBER,
    DATE,
    DROPDOWN,
    CHECKBOX,
    RADIO
}