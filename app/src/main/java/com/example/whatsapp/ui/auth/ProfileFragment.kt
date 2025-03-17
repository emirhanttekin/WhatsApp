package com.example.whatsapp.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.whatsapp.R
import com.example.whatsapp.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var binding: FragmentProfileBinding
    private val profileViewModel: ProfileViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentProfileBinding.bind(view)

        observeViewModel()

        binding.btnEditProfile.setOnClickListener {
            toggleEditMode(true)
        }

        binding.btnSaveProfile.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val surname = binding.etSurname.text.toString().trim()


            profileViewModel.updateUserProfile(name, surname)
        }

        binding.btnLogout.setOnClickListener {
            profileViewModel.signOut()
            findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
        }
    }

    private fun observeViewModel() {
        profileViewModel.userProfile.observe(viewLifecycleOwner, Observer { user ->
            if (user != null) {
                binding.etName.setText(user.name)
                binding.etSurname.setText(user.surname)


                if (user.profileImageUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(user.profileImageUrl)
                        .circleCrop()
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .into(binding.imgProfile)
                }
            }
        })

        profileViewModel.updateStatus.observe(viewLifecycleOwner, Observer { success ->
            if (success) {
                Toast.makeText(requireContext(), "Profil Güncellendi!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_profileFragment_to_groupListFragment)
            } else {
                Toast.makeText(requireContext(), "Güncelleme Başarısız!", Toast.LENGTH_SHORT).show()
            }
        })

        profileViewModel.loadUserProfile()
    }

    private fun toggleEditMode(enable: Boolean) {
        binding.etName.isEnabled = enable
        binding.etSurname.isEnabled = enable

        binding.btnSaveProfile.isVisible = enable
    }
}
