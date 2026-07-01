package com.example.offlineforms.Navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.offlineforms.ui.screens.LoginScreen
import com.example.offlineforms.ui.screens.HomeScreen
import com.example.offlineforms.ui.screens.FormBuilderScreen
import com.example.offlineforms.ui.screens.FormPreviewScreen
import com.example.offlineforms.ui.screens.FillFormScreen
import com.example.offlineforms.ui.screens.ResponsesScreen
import com.example.offlineforms.ui.screens.ResponseDetailScreen
import com.example.offlineforms.ui.screens.NoInternetScreen
import com.example.offlineforms.ui.screens.StartupScreen
import com.example.offlineforms.ui.viewmodel.FormViewModel
import com.example.offlineforms.ui.screens.ImportsScreen
import com.example.offlineforms.ui.screens.FillImportedFormScreen

object Routes {
    const val STARTUP = "startup"
    const val LOGIN = "login"
    const val HOME = "home"
    const val FORM_BUILDER = "form_builder"
    const val FORM_BUILDER_EDIT = "form_builder/{formId}"
    const val FORM_PREVIEW = "form_preview/{formId}"
    const val FILL_FORM = "fill_form/{formId}"
    const val RESPONSES = "responses/{formId}"
    const val RESPONSE_DETAIL = "response_detail/{responseId}"
    const val NO_INTERNET = "no_internet"

    const val IMPORTS = "imports"
    const val FILL_IMPORTED = "fill_imported/{importId}"
}

@Composable
fun NavGraph(formViewModel: FormViewModel = viewModel()) {
    val navController = rememberNavController()


    NavHost(
        navController = navController,
        startDestination = Routes.STARTUP
    ) {
        // Startup route — invisible screen that handles auth init
        composable(Routes.STARTUP) {
            StartupScreen(
                formViewModel = formViewModel,
                onReady = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.STARTUP) { inclusive = true }
                    }
                },
                onNoInternet = {
                    navController.navigate(Routes.NO_INTERNET) {
                        popUpTo(Routes.STARTUP) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.NO_INTERNET) {
            NoInternetScreen(
                onRetry = {
                    navController.navigate(Routes.STARTUP) {
                        popUpTo(Routes.NO_INTERNET) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                navController = navController,
                formViewModel = formViewModel
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                navController = navController,
                formViewModel = formViewModel
            )
        }

        composable(Routes.FORM_BUILDER) {
            FormBuilderScreen(
                navController = navController,
                formId = null,
                formViewModel = formViewModel
            )
        }

        composable(Routes.FORM_BUILDER_EDIT) { backStackEntry ->
            val formId = backStackEntry.arguments?.getString("formId")
            FormBuilderScreen(
                navController = navController,
                formId = formId,
                formViewModel = formViewModel
            )
        }

        composable(Routes.FORM_PREVIEW) { backStackEntry ->
            val formId = backStackEntry.arguments?.getString("formId")
                ?: return@composable
            FormPreviewScreen(
                navController = navController,
                formId = formId,
                formViewModel = formViewModel
            )
        }

        composable(Routes.FILL_FORM) { backStackEntry ->
            val formId = backStackEntry.arguments?.getString("formId")
                ?: return@composable
            FillFormScreen(
                navController = navController,
                formId = formId,
                formViewModel = formViewModel
            )
        }

        composable(Routes.RESPONSES) { backStackEntry ->
            val formId = backStackEntry.arguments?.getString("formId")
                ?: return@composable
            ResponsesScreen(
                navController = navController,
                formId = formId,
                formViewModel = formViewModel
            )
        }

        composable(Routes.RESPONSE_DETAIL) { backStackEntry ->
            val responseId = backStackEntry.arguments?.getString("responseId")
                ?: return@composable
            ResponseDetailScreen(
                navController = navController,
                responseId = responseId,
                formViewModel = formViewModel
            )
        }

        composable(Routes.IMPORTS) {
            ImportsScreen(
                navController = navController,
                formViewModel = formViewModel
            )
        }

        composable(Routes.FILL_IMPORTED) { backStackEntry ->
            val importId = backStackEntry.arguments?.getString("importId")
                ?: return@composable
            FillImportedFormScreen(
                navController = navController,
                importId = importId,
                formViewModel = formViewModel
            )
        }
    }
}