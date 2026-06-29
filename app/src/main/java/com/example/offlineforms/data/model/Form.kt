package com.example.offlineforms.data.model

// A Form is the template.

data class Form(
    val id: String = "",         //Firestore document ID, uniquely identifies this form
    val title: String = "",      //the form's name shown on the home screen card
    val fields: List<FormField> = emptyList(),        //the list of questions inside this form
    val createdAt: Long = System.currentTimeMillis(),  //timestamps so we can show "edited 2h ago" on the card
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,  // false means saved locally only
    val userId: String = ""         //ties the form to the logged-in Firebase use
)