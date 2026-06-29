package com.example.offlineforms.Navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.offlineforms.ui.screens.LoginScreen
import com.example.offlineforms.ui.screens.HomeScreen
import com.example.offlineforms.ui.screens.FormBuilderScreen
import com.example.offlineforms.ui.screens.FormPreviewScreen
import com.example.offlineforms.ui.screens.FormFillScreen
import com.example.offlineforms.ui.screens.ResponsesScreen
import com.example.offlineforms.ui.screens.ResponseDetailScreen

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val FORM_BUILDER = "form_builder"
    const val FORM_BUILDER_EDIT = "form_builder/{formId}"
    const val FORM_PREVIEW = "form_preview/{formId}"
    const val FILL_FORM = "fill_form/{formId}"
    const val RESPONSES = "responses/{formId}"
    const val RESPONSE_DETAIL = "response_detail/{responseId}"
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(navController = navController)
        }
        composable(Routes.HOME) {
            HomeScreen(navController = navController)
        }
        composable(Routes.FORM_BUILDER) {
            FormBuilderScreen(navController = navController, formId = null)
        }
        composable(Routes.FORM_BUILDER_EDIT) { backStackEntry ->
            val formId = backStackEntry.arguments?.getString("formId")
            FormBuilderScreen(navController = navController, formId = formId)
        }
        composable(Routes.FORM_PREVIEW) { backStackEntry ->
            val formId = backStackEntry.arguments?.getString("formId") ?: return@composable
            FormPreviewScreen(navController = navController, formId = formId)
        }
        composable(Routes.FILL_FORM) { backStackEntry ->
            val formId = backStackEntry.arguments?.getString("formId") ?: return@composable
            FormFillScreen(navController = navController, formId = formId)
        }
        composable(Routes.RESPONSES) { backStackEntry ->
            val formId = backStackEntry.arguments?.getString("formId") ?: return@composable
            ResponsesScreen(navController = navController, formId = formId)
        }
        composable(Routes.RESPONSE_DETAIL) { backStackEntry ->
            val responseId = backStackEntry.arguments?.getString("responseId") ?: return@composable
            ResponseDetailScreen(navController = navController, responseId = responseId)
        }
    }
}