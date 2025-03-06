package com.example.whatsapp.ui.company

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.whatsapp.R
import com.example.whatsapp.data.model.Company
import com.example.whatsapp.databinding.FragmentSelectCompanyBinding
import com.example.whatsapp.ui.company.adapter.CompanyAdapter
import com.example.whatsapp.ui.company.viewmodel.CompanyViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SelectCompanyFragment : Fragment(R.layout.fragment_select_company) {

    private lateinit var binding: FragmentSelectCompanyBinding
    private val viewModel: CompanyViewModel by viewModels()
    private lateinit var companyAdapter: CompanyAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSelectCompanyBinding.bind(view)

        setupRecyclerView()
        observeCompanies()

        // Firebase'den şirketleri çek
        viewModel.fetchCompanies()

        binding.btnCreateCompany.setOnClickListener {
            findNavController().navigate(R.id.action_selectCompanyFragment_to_createCompanyFragment)
        }
    }

    private fun setupRecyclerView() {
        companyAdapter = CompanyAdapter(emptyList()) { company ->
            onCompanySelected(company)
        }
        binding.rvCompanyList.apply {
            layoutManager = LinearLayoutManager(requireContext()) // LayoutManager ekledik
            adapter = companyAdapter
        }
    }

    private fun observeCompanies() {
        viewModel.companies.observe(viewLifecycleOwner) { companyList ->
            Log.d("SelectCompanyFragment", "Gelen şirket sayısı: ${companyList.size}")
            companyAdapter.updateList(companyList)  // Güncelleme fonksiyonunu çağır
        }
    }

    private fun onCompanySelected(company: Company) {
        Toast.makeText(requireContext(), "${company.name} seçildi", Toast.LENGTH_SHORT).show()
        findNavController().navigate(R.id.action_selectCompanyFragment_to_homeFragment)
    }
}
