package com.example.whatsapp.ui.company

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.whatsapp.R
import com.example.whatsapp.databinding.FragmentCreateCompanyBinding
import com.example.whatsapp.ui.company.viewmodel.CreateCompanyViewModel
import com.example.whatsapp.utils.Resource
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateCompanyFragment : Fragment(R.layout.fragment_create_company) {

    private val viewModel: CreateCompanyViewModel by viewModels()
    private lateinit var binding: FragmentCreateCompanyBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentCreateCompanyBinding.bind(view)

        binding.btnCreateCompany.setOnClickListener {
            val companyName = binding.etCompanyName.text.toString().trim()
            val companyDescription = binding.etCompanyDescription.text.toString().trim()

            viewModel.createCompany(companyName, companyDescription)
        }

        viewModel.createCompanyState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnCreateCompany.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnCreateCompany.isEnabled = true
                    val companyId = state.data ?: "" // ✅ Eğer `companyId` null ise boş string ata
                    Toast.makeText(requireContext(), "Şirket başarıyla oluşturuldu!", Toast.LENGTH_LONG).show()

                    val action = CreateCompanyFragmentDirections
                        .actionCreateCompanyFragmentToCreateGroupFragment(companyId) // ✅ Null olmamasını sağladık
                    findNavController().navigate(action)
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnCreateCompany.isEnabled = true
                    Toast.makeText(requireContext(), state.message ?: "Şirket oluşturulamadı!", Toast.LENGTH_LONG).show()
                }
            }
        }




    }
}
