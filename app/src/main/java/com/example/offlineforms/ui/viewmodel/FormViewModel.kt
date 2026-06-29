package com.example.offlineforms.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.offlineforms.data.model.Form
import com.example.offlineforms.data.model.FormSubmission
import com.example.offlineforms.data.repository.FormRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FormViewModel : ViewModel() {

    private val repository = FormRepository()

    // ─────────────────────────────────────────
    // STATE - what the UI observes
    // ─────────────────────────────────────────

    // List of all forms shown on home screen
    private val _forms = MutableStateFlow<List<Form>>(emptyList())
    val forms: StateFlow<List<Form>> = _forms.asStateFlow()

    // The form currently being built or edited
    private val _currentForm = MutableStateFlow<Form?>(null)
    val currentForm: StateFlow<Form?> = _currentForm.asStateFlow()

    // List of submissions for a specific form
    private val _submissions = MutableStateFlow<List<FormSubmission>>(emptyList())
    val submissions: StateFlow<List<FormSubmission>> = _submissions.asStateFlow()

    // Single submission being viewed
    private val _currentSubmission = MutableStateFlow<FormSubmission?>(null)
    val currentSubmission: StateFlow<FormSubmission?> = _currentSubmission.asStateFlow()

    // Loading state - shows a spinner while waiting for data
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error state - shows error messages to the user
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Auth state - is user logged in?
    private val _isLoggedIn = MutableStateFlow(repository.isUserLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // ─────────────────────────────────────────
    // FORM OPERATIONS
    // ─────────────────────────────────────────

    // Load all forms for the home screen
    // Called once when HomeScreen appears
    fun loadForms() {
        viewModelScope.launch {
            repository.getForms().collect { formList ->
                _forms.value = formList
            }
        }
    }

    // Load a specific form for editing or previewing
    fun loadFormById(formId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val form = repository.getFormById(formId)
            _currentForm.value = form
            _isLoading.value = false
        }
    }

    // Save a form (works for both new and existing forms)
    fun saveForm(form: Form, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.saveForm(form)
            _isLoading.value = false
            if (result.isSuccess) {
                onSuccess()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message
                    ?: "Failed to save form"
            }
        }
    }

    // Delete a form
    fun deleteForm(formId: String) {
        viewModelScope.launch {
            val result = repository.deleteForm(formId)
            if (result.isFailure) {
                _errorMessage.value = "Failed to delete form"
            }
        }
    }

    // Set the form being built in the builder screen
    fun setCurrentForm(form: Form) {
        _currentForm.value = form
    }

    // Clear the current form when leaving the builder
    fun clearCurrentForm() {
        _currentForm.value = null
    }

    // ─────────────────────────────────────────
    // SUBMISSION OPERATIONS
    // ─────────────────────────────────────────

    // Load all submissions for a specific form
    fun loadSubmissions(formId: String) {
        viewModelScope.launch {
            repository.getSubmissions(formId).collect { submissionList ->
                _submissions.value = submissionList
            }
        }
    }

    // Load a single submission for the detail screen
    fun loadSubmissionById(submissionId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val submission = repository.getSubmissionById(submissionId)
            _currentSubmission.value = submission
            _isLoading.value = false
        }
    }

    // Save a filled form response
    fun saveSubmission(submission: FormSubmission, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.saveSubmission(submission)
            _isLoading.value = false
            if (result.isSuccess) {
                onSuccess()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message
                    ?: "Failed to save response"
            }
        }
    }

    // Delete a submission
    fun deleteSubmission(submissionId: String) {
        viewModelScope.launch {
            val result = repository.deleteSubmission(submissionId)
            if (result.isFailure) {
                _errorMessage.value = "Failed to delete response"
            }
        }
    }

    // ─────────────────────────────────────────
    // AUTH OPERATIONS
    // ─────────────────────────────────────────

    fun signIn(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.signInWithEmail(email, password)
            _isLoading.value = false
            if (result.isSuccess) {
                _isLoggedIn.value = true
                onSuccess()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message
                    ?: "Sign in failed"
            }
        }
    }

    fun signUp(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.signUpWithEmail(email, password)
            _isLoading.value = false
            if (result.isSuccess) {
                _isLoggedIn.value = true
                onSuccess()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message
                    ?: "Sign up failed"
            }
        }
    }

    fun signOut(onSuccess: () -> Unit) {
        repository.signOut()
        _isLoggedIn.value = false
        _forms.value = emptyList()
        onSuccess()
    }

    // Clear error message after it's been shown
    fun clearError() {
        _errorMessage.value = null
    }
}