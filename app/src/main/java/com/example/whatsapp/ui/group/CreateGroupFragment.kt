package com.example.whatsapp.ui.group

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.whatsapp.R
import com.example.whatsapp.databinding.FragmentCreateGroupBinding
import com.example.whatsapp.ui.company.viewmodel.CreateGroupViewModel
import com.example.whatsapp.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint



@AndroidEntryPoint
class CreateGroupFragment : Fragment(R.layout.fragment_create_group) {

    private val viewModel: CreateGroupViewModel by viewModels()
    private lateinit var binding: FragmentCreateGroupBinding
    private var companyId: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentCreateGroupBinding.bind(view)


        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    companyId = document.getString("companyId") ?: ""
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Şirket ID alınamadı!", Toast.LENGTH_SHORT).show()
            }

        binding.btnCreateGroup.setOnClickListener {
            val groupName = binding.etGroupName.text.toString().trim()

            if (groupName.isNotEmpty() && companyId.isNotEmpty()) {
                viewModel.createGroup(companyId, groupName)
            } else {
                Toast.makeText(requireContext(), "Şirket ID eksik!", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.createGroupState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnCreateGroup.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnCreateGroup.isEnabled = true
                    Toast.makeText(requireContext(), "Grup başarıyla oluşturuldu!", Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.action_createGroupFragment_to_groupListFragment)
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnCreateGroup.isEnabled = true
                    Toast.makeText(requireContext(), state.message ?: "Grup oluşturulamadı!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

