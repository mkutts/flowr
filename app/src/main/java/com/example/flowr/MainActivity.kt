// MainActivity.kt

package com.example.flowr
import android.content.Intent

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flowr.adapters.StrainAdapter
import com.example.flowr.models.Strain
import com.example.flowr.ui.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var strainAdapter: StrainAdapter
    private var strainList: List<Strain> = listOf() // Holds the full list of strains

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        firestore = FirebaseFirestore.getInstance()
        strainAdapter = StrainAdapter(listOf())

        // Set up RecyclerView
        val strainsRecyclerView = findViewById<RecyclerView>(R.id.strainsRecyclerView)
        strainsRecyclerView.layoutManager = LinearLayoutManager(this)
        strainsRecyclerView.adapter = strainAdapter

        // Load strains from Firestore
        loadStrains()

        // Search functionality
        val searchBar = findViewById<EditText>(R.id.searchBar)
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterStrains(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Filter checkboxes for types
        val checkboxIndica = findViewById<CheckBox>(R.id.checkbox_indica)
        val checkboxSativa = findViewById<CheckBox>(R.id.checkbox_sativa)
        val checkboxHybrid = findViewById<CheckBox>(R.id.checkbox_hybrid)

        // Filter checkboxes for effects
        val checkboxRelaxed = findViewById<CheckBox>(R.id.checkbox_relaxed)
        val checkboxHappy = findViewById<CheckBox>(R.id.checkbox_happy)
        val checkboxEnergetic = findViewById<CheckBox>(R.id.checkbox_energetic)

        // Set listeners for checkboxes to trigger filtering
        val checkboxes = listOf(checkboxIndica, checkboxSativa, checkboxHybrid, checkboxRelaxed, checkboxHappy, checkboxEnergetic)
        for (checkbox in checkboxes) {
            checkbox.setOnCheckedChangeListener { _, _ ->
                filterStrains(searchBar.text.toString())
            }
        }

        // Logout and Add Strain Button setup
        findViewById<Button>(R.id.logoutButton).setOnClickListener { logout() }
        findViewById<Button>(R.id.addStrainButton).setOnClickListener {
            startActivity(Intent(this, AddStrainActivity::class.java))
        }
    }

    private fun loadStrains() {
        firestore.collection("strains").get()
            .addOnSuccessListener { documents ->
                strainList = documents.map { it.toObject(Strain::class.java) }
                strainAdapter.updateData(strainList)
            }
            .addOnFailureListener { exception ->
                Log.e("MainActivity", "Error fetching documents: ", exception)
            }
    }

    private fun filterStrains(query: String) {
        val checkboxIndica = findViewById<CheckBox>(R.id.checkbox_indica)
        val checkboxSativa = findViewById<CheckBox>(R.id.checkbox_sativa)
        val checkboxHybrid = findViewById<CheckBox>(R.id.checkbox_hybrid)
        val checkboxRelaxed = findViewById<CheckBox>(R.id.checkbox_relaxed)
        val checkboxHappy = findViewById<CheckBox>(R.id.checkbox_happy)
        val checkboxEnergetic = findViewById<CheckBox>(R.id.checkbox_energetic)

        // Gather selected filters
        val selectedTypes = mutableListOf<String>()
        if (checkboxIndica.isChecked) selectedTypes.add("indica")
        if (checkboxSativa.isChecked) selectedTypes.add("sativa")
        if (checkboxHybrid.isChecked) selectedTypes.add("hybrid")

        val selectedEffects = mutableListOf<String>()
        if (checkboxRelaxed.isChecked) selectedEffects.add("relaxed")
        if (checkboxHappy.isChecked) selectedEffects.add("happy")
        if (checkboxEnergetic.isChecked) selectedEffects.add("energetic")

        // Filter strains based on search query, type, and effects
        val filteredStrains = strainList.filter { strain ->
            val matchesQuery = strain.name.contains(query, ignoreCase = true)
            val matchesType = selectedTypes.isEmpty() || selectedTypes.contains(strain.type.toLowerCase())
            val matchesEffects = selectedEffects.isEmpty() || selectedEffects.all { it in strain.effects.map { effect -> effect.toLowerCase() } }

            matchesQuery && matchesType && matchesEffects
        }

        strainAdapter.updateData(filteredStrains)
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
