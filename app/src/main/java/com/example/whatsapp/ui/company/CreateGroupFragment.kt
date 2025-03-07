package com.example.whatsapp.ui.company

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.whatsapp.R
import com.example.whatsapp.databinding.FragmentCreateGroupBinding
import com.example.whatsapp.ui.company.viewmodel.CreateGroupViewModel
import com.example.whatsapp.utils.Resource
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateGroupFragment : Fragment(R.layout.fragment_create_group) {

    private val viewModel: CreateGroupViewModel by viewModels()
    private lateinit var binding: FragmentCreateGroupBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentCreateGroupBinding.bind(view)

        binding.btnCreateGroup.setOnClickListener {
            val groupName = binding.etGroupName.text.toString().trim()
            val companyId = "Şirket ID’sini burada al"  // Şirket ID’yi almalı

            viewModel.createGroup(groupName, companyId)
        }
    }
}
