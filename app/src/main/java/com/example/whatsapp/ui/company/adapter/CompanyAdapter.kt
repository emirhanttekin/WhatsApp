package com.example.whatsapp.ui.company.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.whatsapp.data.model.Company
import com.example.whatsapp.databinding.ItemCompanyBinding

class CompanyAdapter(
    private var companyList: List<Company>,
    private val onItemClick: (Company) -> Unit
) : RecyclerView.Adapter<CompanyAdapter.CompanyViewHolder>() {

    class CompanyViewHolder(val binding: ItemCompanyBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompanyViewHolder {
        val binding = ItemCompanyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CompanyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CompanyViewHolder, position: Int) {
        val company = companyList[position]
        holder.binding.tvCompanyName.text = company.name

        // Buton yerine tüm öğeye tıklanabilirlik ekleyelim
        holder.itemView.setOnClickListener { onItemClick(company) }
    }

    override fun getItemCount(): Int = companyList.size

    fun updateList(newList: List<Company>) {
        companyList = newList.toList()
        notifyDataSetChanged()
    }
}
