package com.example.flowr.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.flowr.R
import com.example.flowr.models.Strain

class StrainAdapter(private var strains: List<Strain>) : RecyclerView.Adapter<StrainAdapter.StrainViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StrainViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_strain, parent, false)
        return StrainViewHolder(view)
    }

    override fun onBindViewHolder(holder: StrainViewHolder, position: Int) {
        val strain = strains[position]
        holder.nameTextView.text = strain.name
        holder.typeTextView.text = strain.type
    }

    override fun getItemCount(): Int = strains.size

    fun updateData(newStrains: List<Strain>) {
        strains = newStrains
        notifyDataSetChanged()
    }

    class StrainViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.strainName)
        val typeTextView: TextView = itemView.findViewById(R.id.strainType)
    }
}
