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
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.example.offlineforms.data.model.ImportedForm
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

    private val importsCollection = firestore.collection("imports")

    // ─────────────────────────────────────────
    // FORM CRUD OPERATIONS
    // ─────────────────────────────────────────

    // CREATE - Save a new form to Firestore
    // suspend means this runs in the background without freezing the UI
    suspend fun saveForm(form: Form): Result<String> {
        return try {
            val docRef = if (form.id.isEmpty()) {
                formsCollection.document()
            } else {
                formsCollection.document(form.id)
            }

            val formToSave = form.copy(
                id = docRef.id,
                userId = currentUserId,
                updatedAt = System.currentTimeMillis()
            )

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

            // set() with SetOptions.merge() completes instantly using local cache,
            // it does not wait for network confirmation
            docRef.set(formMap).addOnFailureListener { e ->
                android.util.Log.e("FormRepository", "saveForm background failure", e)
            }

            // We don't await() the network round-trip.
            // Firestore's local cache write is synchronous, so we can
            // confidently return success right after triggering the write.
            Result.success(docRef.id)
        } catch (e: Exception) {
            android.util.Log.e("FormRepository", "saveForm failed", e)
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
                    val isFromCache = doc.metadata.isFromCache
                    val form = doc.toForm()

                    // If this read is confirmed from server (not cache) and
                    // the form wasn't already marked synced, update it permanently
                    if (!isFromCache && form != null && !form.isSynced) {
                        doc.reference.update("isSynced", true)
                    }

                    // Show as synced if either:
                    // - this read came from server (definitely synced), OR
                    // - the document already has isSynced = true stored from before
                    form?.copy(isSynced = form.isSynced || !isFromCache)
                } ?: emptyList()
                trySend(forms)
            }
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

            docRef.set(submissionMap).addOnFailureListener { e ->
                android.util.Log.e("FormRepository", "saveSubmission background failure", e)
            }

            Result.success(docRef.id)
        } catch (e: Exception) {
            android.util.Log.e("FormRepository", "saveSubmission failed", e)
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
                    val isFromCache = doc.metadata.isFromCache
                    val submission = doc.toSubmission()

                    if (!isFromCache && submission != null && !submission.isSynced) {
                        doc.reference.update("isSynced", true)
                    }

                    submission?.copy(isSynced = submission.isSynced || !isFromCache)
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

    suspend fun signInOrLink(email: String, password: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser != null && currentUser.isAnonymous) {
                // Anonymous user upgrading to real account
                try {
                    val credential = com.google.firebase.auth.EmailAuthProvider
                        .getCredential(email, password)
                    currentUser.linkWithCredential(credential).await()
                    Result.success(Unit)
                } catch (e: Exception) {
                    // Link failed — they likely already have an account
                    // Sign in normally instead
                    auth.signInWithEmailAndPassword(email, password).await()
                    Result.success(Unit)
                }
            } else {
                // No anonymous session — just sign in normally
                auth.signInWithEmailAndPassword(email, password).await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            android.util.Log.e("FormRepository", "signInOrLink failed", e)
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, password: String): Result<Unit> {
        return try {
            auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FormRepository", "signUp failed", e)
            Result.failure(e)
        }
    }

    // Signs in anonymously — no email/password needed
// Firebase creates a temporary account automatically
// This works silently in the background on first launch

    suspend fun signInAnonymously(): Result<Unit> {
        return try {
            auth.signInAnonymously().await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FormRepository", "Anonymous sign in failed", e)
            Result.failure(e)
        }
    }

    // Checks if current user is anonymous (not signed into a real account)
    fun isUserAnonymous(): Boolean {
        return auth.currentUser?.isAnonymous ?: true
    }

    // Links anonymous account to real email account
// This preserves all locally saved forms when user decides to sign up
    suspend fun linkAnonymousToEmail(email: String, password: String): Result<Unit> {
        return try {
            val credential = com.google.firebase.auth.EmailAuthProvider
                .getCredential(email, password)
            auth.currentUser?.linkWithCredential(credential)?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FormRepository", "Account linking failed", e)
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

    private fun DocumentSnapshot.toImportedForm(): ImportedForm? {
        return try {
            val fieldsData = get("fields") as? List<Map<String, Any>> ?: emptyList()
            ImportedForm(
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
                importedAt = getLong("importedAt") ?: 0L,
                originalCreatorId = getString("originalCreatorId") ?: "",
                originalFormId = getString("originalFormId") ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun DocumentSnapshot.toForm(): Form? {
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

    private fun DocumentSnapshot.toSubmission(): FormSubmission? {
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

    // ─────────────────────────────────────────
// EXPORT / IMPORT OPERATIONS
// ─────────────────────────────────────────

    // Converts a Form to a JSON string for sharing
    fun exportFormToJson(form: Form): String {
        val fieldsJson = form.fields.joinToString(",") { field ->
            """
        {
            "id": "${field.id}",
            "label": "${field.label}",
            "type": "${field.type.name}",
            "isRequired": ${field.isRequired},
            "options": [${field.options.joinToString(",") { "\"$it\"" }}]
        }
        """.trimIndent()
        }

        return """
    {
        "offlineFormsExport": true,
        "id": "${form.id}",
        "title": "${form.title}",
        "creatorUserId": "${form.userId}",
        "createdAt": ${form.createdAt},
        "fields": [$fieldsJson]
    }
    """.trimIndent()
    }

    // Parses a JSON string back into an ImportedForm
    fun parseImportedForm(jsonString: String): ImportedForm? {
        return try {
            // Basic JSON parsing without external library
            fun extractString(json: String, key: String): String {
                val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\""
                val match = Regex(pattern).find(json)
                return match?.groupValues?.get(1) ?: ""
            }

            fun extractBoolean(json: String, key: String): Boolean {
                val pattern = "\"$key\"\\s*:\\s*(true|false)"
                val match = Regex(pattern).find(json)
                return match?.groupValues?.get(1) == "true"
            }

            fun extractLong(json: String, key: String): Long {
                val pattern = "\"$key\"\\s*:\\s*(\\d+)"
                val match = Regex(pattern).find(json)
                return match?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            }

            fun extractArray(json: String, key: String): String {
                val startKey = "\"$key\""
                val startIndex = json.indexOf(startKey)
                if (startIndex == -1) return "[]"
                val arrayStart = json.indexOf("[", startIndex)
                if (arrayStart == -1) return "[]"
                var depth = 0
                var i = arrayStart
                while (i < json.length) {
                    when (json[i]) {
                        '[' -> depth++
                        ']' -> {
                            depth--
                            if (depth == 0) return json.substring(arrayStart, i + 1)
                        }
                    }
                    i++
                }
                return "[]"
            }

            fun extractObjects(arrayJson: String): List<String> {
                val objects = mutableListOf<String>()
                var depth = 0
                var start = -1
                for (i in arrayJson.indices) {
                    when (arrayJson[i]) {
                        '{' -> {
                            if (depth == 0) start = i
                            depth++
                        }
                        '}' -> {
                            depth--
                            if (depth == 0 && start != -1) {
                                objects.add(arrayJson.substring(start, i + 1))
                            }
                        }
                    }
                }
                return objects
            }

            fun extractStringList(arrayJson: String): List<String> {
                return Regex("\"([^\"]*)\"").findAll(arrayJson)
                    .map { it.groupValues[1] }
                    .toList()
            }

            // Verify it's a valid OfflineForms export
            if (!jsonString.contains("\"offlineFormsExport\": true")) return null

            val fieldsArrayJson = extractArray(jsonString, "fields")
            val fieldObjects = extractObjects(fieldsArrayJson)

            val fields = fieldObjects.map { fieldJson ->
                val optionsArrayJson = extractArray(fieldJson, "options")
                FormField(
                    id = extractString(fieldJson, "id"),
                    label = extractString(fieldJson, "label"),
                    type = try {
                        FieldType.valueOf(extractString(fieldJson, "type"))
                    } catch (e: Exception) {
                        FieldType.TEXT
                    },
                    isRequired = extractBoolean(fieldJson, "isRequired"),
                    options = extractStringList(optionsArrayJson)
                )
            }

            ImportedForm(
                id = java.util.UUID.randomUUID().toString(),
                title = extractString(jsonString, "title"),
                fields = fields,
                importedAt = System.currentTimeMillis(),
                originalCreatorId = extractString(jsonString, "creatorUserId"),
                originalFormId = extractString(jsonString, "id")
            )
        } catch (e: Exception) {
            android.util.Log.e("FormRepository", "parseImportedForm failed", e)
            null
        }
    }

    // Save imported form to Firestore under "imports" collection
    suspend fun saveImportedForm(importedForm: ImportedForm): Result<String> {
        return try {
            val docRef = importsCollection.document(importedForm.id)
            val importMap = mapOf(
                "id" to importedForm.id,
                "title" to importedForm.title,
                "fields" to importedForm.fields.map { field ->
                    mapOf(
                        "id" to field.id,
                        "label" to field.label,
                        "type" to field.type.name,
                        "isRequired" to field.isRequired,
                        "options" to field.options
                    )
                },
                "importedAt" to importedForm.importedAt,
                "originalCreatorId" to importedForm.originalCreatorId,
                "originalFormId" to importedForm.originalFormId,
                "userId" to currentUserId
            )
            docRef.set(importMap).addOnFailureListener { e ->
                android.util.Log.e("FormRepository", "saveImportedForm background failure", e)
            }
            Result.success(importedForm.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get all imported forms as a live stream
    fun getImportedForms(): Flow<List<ImportedForm>> = callbackFlow {
        val listener = importsCollection
            .whereEqualTo("userId", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val imports = snapshot?.documents?.mapNotNull { doc ->
                    doc.toImportedForm()
                } ?: emptyList()
                trySend(imports)
            }
        awaitClose { listener.remove() }
    }

    // Delete an imported form
    suspend fun deleteImportedForm(importId: String): Result<Unit> {
        return try {
            importsCollection.document(importId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}