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
    private val currentUid: String
        get() = auth.currentUser?.uid ?: ""

    // Reference to the "forms" collection in Firestore
    private val formsCollection = firestore.collection("forms")

    // Reference to the "submissions" collection in Firestore
    private val submissionsCollection = firestore.collection("submissions")

    private val importsCollection = firestore.collection("imports")

    // Get the current user ID
    fun getCurrentUserId(): String = currentUid

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

            // Always use currentUid to ensure ownership, unless explicitly set (migration case)
            val formUserId = if (form.userId.isNotEmpty()) form.userId else currentUid

            val formToSave = form.copy(
                id = docRef.id,
                userId = formUserId,
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
            docRef.set(formMap).await() // Use await() to ensure local write is confirmed

            Result.success(docRef.id)
        } catch (e: Exception) {
            android.util.Log.e("FormRepository", "saveForm failed", e)
            Result.failure(e)
        }
    }

    // READ - Get all forms for the current user as a real-time stream
// Flow means the UI automatically updates whenever data changes
    fun getForms(userId: String): Flow<List<Form>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(emptyList())
            return@callbackFlow
        }
        val listener = formsCollection
            .whereEqualTo("userId", userId)
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

            val submissionUserId = if (submission.userId.isNotEmpty()) submission.userId else currentUid

            val submissionToSave = submission.copy(
                id = docRef.id,
                userId = submissionUserId
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
            android.util.Log.e("FormRepository", "saveSubmission failed", e)
            Result.failure(e)
        }
    }

    // READ - Get all submissions for a specific form as a real-time stream
    fun getSubmissions(formId: String, userId: String): Flow<List<FormSubmission>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(emptyList())
            return@callbackFlow
        }
        val listener = submissionsCollection
            .whereEqualTo("formId", formId)
            .whereEqualTo("userId", userId)
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
            val oldUid = currentUser?.uid ?: ""
            val isAnonymous = currentUser?.isAnonymous ?: false

            if (currentUser != null && isAnonymous) {
                // 1. Fetch current anonymous data while we still have permission
                val forms = formsCollection.whereEqualTo("userId", oldUid).get().await()
                    .documents.mapNotNull { it.toForm() }
                val submissions = submissionsCollection.whereEqualTo("userId", oldUid).get().await()
                    .documents.mapNotNull { it.toSubmission() }
                val imports = importsCollection.whereEqualTo("userId", oldUid).get().await()
                    .documents.mapNotNull { it.toImportedForm() }

                try {
                    val credential = com.google.firebase.auth.EmailAuthProvider
                        .getCredential(email, password)
                    currentUser.linkWithCredential(credential).await()
                    // Linking worked! UID is the same, no data migration needed.
                    Result.success(Unit)
                } catch (linkError: Exception) {
                    // Linking failed (collision). Sign in to the existing account.
                    auth.signInWithEmailAndPassword(email, password).await()
                    val newUid = auth.currentUser?.uid ?: ""

                    if (newUid.isNotEmpty() && newUid != oldUid) {
                        // 2. Upload the anonymous data to the new account
                        migrateDataToNewAccount(newUid, forms, submissions, imports)
                    }
                    Result.success(Unit)
                }
            } else {
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
            val currentUser = auth.currentUser
            val oldUid = currentUser?.uid ?: ""
            val isAnonymous = currentUser?.isAnonymous ?: false

            if (currentUser != null && isAnonymous) {
                // Fetch data before creating new user
                val forms = formsCollection.whereEqualTo("userId", oldUid).get().await()
                    .documents.mapNotNull { it.toForm() }
                val submissions = submissionsCollection.whereEqualTo("userId", oldUid).get().await()
                    .documents.mapNotNull { it.toSubmission() }
                val imports = importsCollection.whereEqualTo("userId", oldUid).get().await()
                    .documents.mapNotNull { it.toImportedForm() }

                val credential = com.google.firebase.auth.EmailAuthProvider
                    .getCredential(email, password)

                try {
                    currentUser.linkWithCredential(credential).await()
                } catch (linkError: Exception) {
                    auth.createUserWithEmailAndPassword(email, password).await()
                    val newUid = auth.currentUser?.uid ?: ""
                    if (newUid.isNotEmpty() && newUid != oldUid) {
                        migrateDataToNewAccount(newUid, forms, submissions, imports)
                    }
                }
            } else {
                auth.createUserWithEmailAndPassword(email, password).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FormRepository", "signUp failed", e)
            Result.failure(e)
        }
    }

    private suspend fun migrateDataToNewAccount(
        newUid: String,
        forms: List<Form>,
        submissions: List<FormSubmission>,
        imports: List<ImportedForm>
    ) {
        try {
            // Re-save all previously anonymous items under the new permanent UID
            forms.forEach { form ->
                saveForm(form.copy(userId = newUid, isSynced = false))
            }
            submissions.forEach { sub ->
                saveSubmission(sub.copy(userId = newUid, isSynced = false))
            }
            imports.forEach { imp ->
                saveImportedForm(imp) // saveImportedForm already uses currentUid internally
            }
            android.util.Log.d("FormRepository", "Successfully migrated ${forms.size} forms to account $newUid")
        } catch (e: Exception) {
            android.util.Log.e("FormRepository", "migrateDataToNewAccount failed", e)
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
            val fieldsData = get("fields") as? List<*> ?: emptyList<Any>()
            ImportedForm(
                id = getString("id") ?: "",
                title = getString("title") ?: "",
                fields = fieldsData.filterIsInstance<Map<String, Any>>().map { fieldMap ->
                    FormField(
                        id = fieldMap["id"] as? String ?: "",
                        label = fieldMap["label"] as? String ?: "",
                        type = try {
                            FieldType.valueOf(fieldMap["type"] as? String ?: "TEXT")
                        } catch (e: Exception) {
                            FieldType.TEXT
                        },
                        isRequired = fieldMap["isRequired"] as? Boolean ?: false,
                        options = (fieldMap["options"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
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
            val fieldsData = get("fields") as? List<*> ?: emptyList<Any>()
            Form(
                id = getString("id") ?: "",
                title = getString("title") ?: "",
                fields = fieldsData.filterIsInstance<Map<String, Any>>().map { fieldMap ->
                    FormField(
                        id = fieldMap["id"] as? String ?: "",
                        label = fieldMap["label"] as? String ?: "",
                        type = try {
                            FieldType.valueOf(fieldMap["type"] as? String ?: "TEXT")
                        } catch (e: Exception) {
                            FieldType.TEXT
                        },
                        isRequired = fieldMap["isRequired"] as? Boolean ?: false,
                        options = (fieldMap["options"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
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
            val answersData = get("answers") as? Map<*, *> ?: emptyMap<Any, Any>()
            FormSubmission(
                id = getString("id") ?: "",
                formId = getString("formId") ?: "",
                formTitle = getString("formTitle") ?: "",
                answers = answersData.map { it.key.toString() to it.value.toString() }.toMap(),
                submittedAt = getLong("submittedAt") ?: 0L,
                isSynced = getBoolean("isSynced") ?: false,
                userId = getString("userId") ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }

    // Converts a Form to a JSON string for sharing
    fun exportFormToJson(form: Form): String {
        fun escape(s: String): String {
            return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
        }

        val fieldsJson = form.fields.joinToString(",") { field ->
            val optionsJson = field.options.joinToString(",") { "\"${escape(it)}\"" }
            """
            {
                "id": "${escape(field.id)}",
                "label": "${escape(field.label)}",
                "type": "${field.type.name}",
                "isRequired": ${field.isRequired},
                "options": [$optionsJson]
            }
            """.trimIndent()
        }

        return """
        {
            "offlineFormsExport": true,
            "id": "${escape(form.id)}",
            "title": "${escape(form.title)}",
            "creatorUserId": "${escape(form.userId)}",
            "createdAt": ${form.createdAt},
            "fields": [$fieldsJson]
        }
        """.trimIndent()
    }

    // Parses a JSON string back into an ImportedForm
    fun parseImportedForm(jsonString: String): ImportedForm? {
        return try {
            fun unescape(s: String): String {
                return s.replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t")
            }

            fun extractString(json: String, key: String): String {
                // Look for "key" : "value"
                // The value regex handles escaped quotes: (?:[^"\\]|\\.)*
                val pattern = "\"$key\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""
                val match = Regex(pattern).find(json)
                return unescape(match?.groupValues?.get(1) ?: "")
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

            // Find the "fields" array content
            val fieldsMatch = Regex("\"fields\"\\s*:\\s*\\[([\\s\\S]*)\\]").find(jsonString)
            val fieldsArrayContent = fieldsMatch?.groupValues?.get(1) ?: ""

            // Split the array content into individual field objects {}
            // We look for { ... } at the top level of the array content
            val fieldObjects = mutableListOf<String>()
            var depth = 0
            var start = -1
            for (i in fieldsArrayContent.indices) {
                when (fieldsArrayContent[i]) {
                    '{' -> {
                        if (depth == 0) start = i
                        depth++
                    }
                    '}' -> {
                        depth--
                        if (depth == 0 && start != -1) {
                            fieldObjects.add(fieldsArrayContent.substring(start, i + 1))
                        }
                    }
                }
            }

            val fields = fieldObjects.map { fieldJson ->
                // Extract options array from fieldJson
                val optionsMatch = Regex("\"options\"\\s*:\\s*\\[([\\s\\S]*)\\]").find(fieldJson)
                val optionsContent = optionsMatch?.groupValues?.get(1) ?: ""
                val options = Regex("\"((?:[^\"\\\\]|\\\\.)*)\"").findAll(optionsContent)
                    .map { unescape(it.groupValues[1]) }
                    .toList()

                FormField(
                    id = extractString(fieldJson, "id"),
                    label = extractString(fieldJson, "label"),
                    type = try {
                        FieldType.valueOf(extractString(fieldJson, "type"))
                    } catch (e: Exception) {
                        FieldType.TEXT
                    },
                    isRequired = extractBoolean(fieldJson, "isRequired"),
                    options = options
                )
            }

            // Verify it's a valid OfflineForms export
            if (!jsonString.contains("\"offlineFormsExport\": true")) return null

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
                "userId" to currentUid
            )
            docRef.set(importMap).await()
            Result.success(importedForm.id)
        } catch (e: Exception) {
            android.util.Log.e("FormRepository", "saveImportedForm failed", e)
            Result.failure(e)
        }
    }

    // Get all imported forms as a live stream
    fun getImportedForms(userId: String): Flow<List<ImportedForm>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(emptyList())
            return@callbackFlow
        }
        val listener = importsCollection
            .whereEqualTo("userId", userId)
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