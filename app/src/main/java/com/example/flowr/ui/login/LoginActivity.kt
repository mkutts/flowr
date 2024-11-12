// LoginActivity.kt
package com.example.flowr.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.flowr.MainActivity
import com.example.flowr.R
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        // Check if the user is already logged in
        if (auth.currentUser != null) {
            // User is already signed in; go directly to MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Login functionality goes here...
    }
}
