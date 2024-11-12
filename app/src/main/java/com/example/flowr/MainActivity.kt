// MainActivity.kt
package com.example.flowr
import android.widget.EditText  // Required for EditText
import android.text.Editable
import android.text.TextWatcher  // Required for TextWatcher
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
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

        // Set up the search bar (if applicable)
        val searchBar = findViewById<EditText>(R.id.searchBar)
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterStrains(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

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
        val filteredStrains = strainList.filter { strain ->
            strain.name.contains(query, ignoreCase = true)
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
