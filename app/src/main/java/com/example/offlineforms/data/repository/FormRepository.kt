package com.example.offlineforms.data.repository

//This is your database gatekeeper. Instead of writing Firestore code directly inside your
// screens, you write a single function here like saveFormToFirestore(form).
// Your screens will simply call this function when a user taps "Submit."


import com.example.offlineforms.data.model.FieldType
import com.example.offlineforms.data.model.Form
import com.example.offlineforms.data.model.FormField
import com.example.offlineforms.data.model.FormSubmission
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FormRepository {

    // These are our two entry points into Firebase
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Helper to get the current logged-in user's ID
    // We use this to make sure users only see their own forms
    private val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    // Reference to the "forms" collection in Firestore
    private val formsCollection = firestore.collection("forms")

    // Reference to the "submissions" collection in Firestore
    private val submissionsCollection = firestore.collection("submissions")

    // ─────────────────────────────────────────
    // FORM CRUD OPERATIONS
    // ─────────────────────────────────────────

    // CREATE - Save a new form to Firestore
    // suspend means this runs in the background without freezing the UI
    suspend fun saveForm(form: Form): Result<String> {
        return try {
            val docRef = if (form.id.isEmpty()) {
                // New form - let Firestore generate a unique ID
                formsCollection.document()
            } else {
                // Existing form - use its existing ID
                formsCollection.document(form.id)
            }

            val formToSave = form.copy(
                id = docRef.id,
                userId = currentUserId,
                updatedAt = System.currentTimeMillis()
            )

            // Convert FormField list to a format Firestore understands
            val formMap = mapOf(
                "id" to formToSave.id,
                "title" to formToSave.title,
                "fields" to formToSave.fields.map { field ->
                    mapOf(
                        "id" to field.id,
                        "label" to field.label,
                        "type" to field.type.name,
                        "isRequired" to field.isRequired,
                        "options" to field.options
                    )
                },
                "createdAt" to formToSave.createdAt,
                "updatedAt" to formToSave.updatedAt,
                "isSynced" to formToSave.isSynced,
                "userId" to formToSave.userId
            )

            docRef.set(formMap).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // READ - Get all forms for the current user as a real-time stream
    // Flow means the UI automatically updates whenever data changes
    fun getForms(): Flow<List<Form>> = callbackFlow {
        val listener = formsCollection
            .whereEqualTo("userId", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val forms = snapshot?.documents?.mapNotNull { doc ->
                    doc.toForm()
                } ?: emptyList()
                trySend(forms)
            }
        // When the Flow is cancelled, remove the Firestore listener
        awaitClose { listener.remove() }
    }

    // READ - Get a single form by its ID
    suspend fun getFormById(formId: String): Form? {
        return try {
            val doc = formsCollection.document(formId).get().await()
            doc.toForm()
        } catch (e: Exception) {
            null
        }
    }

    // DELETE - Remove a form and all its submissions
    suspend fun deleteForm(formId: String): Result<Unit> {
        return try {
            formsCollection.document(formId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // CREATE - Save a filled form response
    suspend fun saveSubmission(submission: FormSubmission): Result<String> {
        return try {
            val docRef = if (submission.id.isEmpty()) {
                submissionsCollection.document()
            } else {
                submissionsCollection.document(submission.id)
            }

            val submissionToSave = submission.copy(
                id = docRef.id,
                userId = currentUserId
            )

            val submissionMap = mapOf(
                "id" to submissionToSave.id,
                "formId" to submissionToSave.formId,
                "formTitle" to submissionToSave.formTitle,
                "answers" to submissionToSave.answers,
                "submittedAt" to submissionToSave.submittedAt,
                "isSynced" to submissionToSave.isSynced,
                "userId" to submissionToSave.userId
            )

            docRef.set(submissionMap).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // READ - Get all submissions for a specific form as a real-time stream
    fun getSubmissions(formId: String): Flow<List<FormSubmission>> = callbackFlow {
        val listener = submissionsCollection
            .whereEqualTo("formId", formId)
            .whereEqualTo("userId", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val submissions = snapshot?.documents?.mapNotNull { doc ->
                    doc.toSubmission()
                } ?: emptyList()
                trySend(submissions)
            }
        awaitClose { listener.remove() }
    }

    // READ - Get a single submission by its ID
    suspend fun getSubmissionById(submissionId: String): FormSubmission? {
        return try {
            val doc = submissionsCollection.document(submissionId).get().await()
            doc.toSubmission()
        } catch (e: Exception) {
            null
        }
    }

    // DELETE - Remove a submission
    suspend fun deleteSubmission(submissionId: String): Result<Unit> {
        return try {
            submissionsCollection.document(submissionId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────
    // AUTH OPERATIONS
    // ─────────────────────────────────────────

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, password: String): Result<Unit> {
        return try {
            auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    // ─────────────────────────────────────────
    // HELPER FUNCTIONS - Convert Firestore
    // documents back into our data classes
    // ─────────────────────────────────────────

    private fun com.google.firebase.firestore.DocumentSnapshot.toForm(): Form? {
        return try {
            val fieldsData = get("fields") as? List<Map<String, Any>> ?: emptyList()
            Form(
                id = getString("id") ?: "",
                title = getString("title") ?: "",
                fields = fieldsData.map { fieldMap ->
                    FormField(
                        id = fieldMap["id"] as? String ?: "",
                        label = fieldMap["label"] as? String ?: "",
                        type = FieldType.valueOf(
                            fieldMap["type"] as? String ?: "TEXT"
                        ),
                        isRequired = fieldMap["isRequired"] as? Boolean ?: false,
                        options = fieldMap["options"] as? List<String> ?: emptyList()
                    )
                },
                createdAt = getLong("createdAt") ?: 0L,
                updatedAt = getLong("updatedAt") ?: 0L,
                isSynced = getBoolean("isSynced") ?: false,
                userId = getString("userId") ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toSubmission(): FormSubmission? {
        return try {
            FormSubmission(
                id = getString("id") ?: "",
                formId = getString("formId") ?: "",
                formTitle = getString("formTitle") ?: "",
                answers = get("answers") as? Map<String, String> ?: emptyMap(),
                submittedAt = getLong("submittedAt") ?: 0L,
                isSynced = getBoolean("isSynced") ?: false,
                userId = getString("userId") ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }
}