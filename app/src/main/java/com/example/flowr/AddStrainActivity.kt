// AddStrainActivity.kt
package com.example.flowr

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.flowr.models.Strain
import com.google.firebase.firestore.FirebaseFirestore

class AddStrainActivity : AppCompatActivity() {
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_strain)

        firestore = FirebaseFirestore.getInstance()

        val strainNameInput = findViewById<EditText>(R.id.strainNameInput)
        val strainTypeInput = findViewById<EditText>(R.id.strainTypeInput)
        val effectsInput = findViewById<EditText>(R.id.effectsInput)
        val flavorsInput = findViewById<EditText>(R.id.flavorsInput)
        val ratingInput = findViewById<EditText>(R.id.ratingInput)
        val saveStrainButton = findViewById<Button>(R.id.saveStrainButton)

        saveStrainButton.setOnClickListener {
            val name = strainNameInput.text.toString().trim()
            val type = strainTypeInput.text.toString().trim()
            val effects = effectsInput.text.toString().split(",").map { it.trim() }
            val flavors = flavorsInput.text.toString().split(",").map { it.trim() }
            val avgRating = ratingInput.text.toString().toDoubleOrNull() ?: 0.0

            if (name.isEmpty() || type.isEmpty() || avgRating !in 0.0..5.0) {
                Toast.makeText(this, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
            } else {
                saveStrainToFirestore(Strain(name, type, effects, flavors, avgRating))
            }
        }
    }

    private fun saveStrainToFirestore(strain: Strain) {
        firestore.collection("strains").add(strain)
            .addOnSuccessListener {
                Toast.makeText(this, "Strain saved successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save strain: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
