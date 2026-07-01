package com.example.offlineforms.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.offlineforms.ui.viewmodel.FormViewModel

// This screen is never actually seen by the user —
// it shows a brief spinner while we check auth status
// then immediately navigates to Home or NoInternet
@Composable
fun StartupScreen(
    formViewModel: FormViewModel,
    onReady: () -> Unit,
    onNoInternet: () -> Unit
) {
    LaunchedEffect(Unit) {
        formViewModel.initializeAuth(
            onReady = onReady,
            onNoInternet = onNoInternet
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}