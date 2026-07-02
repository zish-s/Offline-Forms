package com.example.offlineforms

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.offlineforms.ui.themes.OfflineFormsTheme
import com.example.offlineforms.Navigation.NavGraph
import com.example.offlineforms.ui.viewmodel.FormViewModel

class MainActivity : ComponentActivity() {

    private val formViewModel: FormViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle file shared when app was not running
        handleIncomingIntent(intent)

        setContent {
            OfflineFormsTheme {
                NavGraph(formViewModel = formViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle file shared when app was already running
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        val action = intent.action
        val type = intent.type

        if ((action == Intent.ACTION_SEND || action == Intent.ACTION_VIEW)
            && (type == "application/json" || type == "*/*")
        ) {
            val uri = if (action == Intent.ACTION_SEND) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            } else {
                intent.data
            }

            uri?.let {
                try {
                    val inputStream = contentResolver.openInputStream(it)
                    val jsonString = inputStream?.bufferedReader()?.readText()
                    inputStream?.close()

                    if (!jsonString.isNullOrEmpty()) {
                        formViewModel.importFormFromJson(
                            jsonString = jsonString,
                            onSuccess = {
                                android.util.Log.d("MainActivity", "Form imported successfully")
                            },
                            onError = {
                                android.util.Log.e("MainActivity", "Form import failed")
                            }
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Failed to read shared file", e)
                }
            }
        }
    }
}