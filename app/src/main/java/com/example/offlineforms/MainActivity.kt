package com.example.offlineforms

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.offlineforms.ui.themes.OfflineFormsTheme
import com.example.offlineforms.Navigation.NavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // lets your UI draw behind the system status bar for a modern full-screen look
        setContent {  //UI starts
            OfflineFormsTheme {
                NavGraph() //decides which screen shows
            }
        }
    }
}