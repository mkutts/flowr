// App.kt
package com.example.flowr

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        Log.d("FirebaseInit", "Firebase has been initialized")
    }
}
