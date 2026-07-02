package com.example.offlineforms

// it's the very first thing that runs when your app opens, even before the maiactivity.kt file, before any screen appears. We use it to initialize Firebase once globally.

import android.app.Application
import com.google.firebase.FirebaseApp

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}