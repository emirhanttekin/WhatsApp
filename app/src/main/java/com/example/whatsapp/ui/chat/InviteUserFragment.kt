package com.example.whatsapp.ui.chat

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.whatsapp.R
import com.example.whatsapp.databinding.FragmentInviteUserBinding
import com.example.whatsapp.ui.chat.viewmodel.GroupInviteViewModel
import com.example.whatsapp.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InviteUserFragment : Fragment(R.layout.fragment_invite_user) {

    private val viewModel: GroupInviteViewModel by viewModels()
    private lateinit var binding: FragmentInviteUserBinding
    private val args: InviteUserFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentInviteUserBinding.bind(view)

        val groupId = args.groupId
        val groupName = args.groupName
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""


        binding.tvGroupName.text = "Grup: $groupName"

        viewModel.checkUserRole(groupId, currentUserId)

        viewModel.userRole.observe(viewLifecycleOwner) { role ->
            if (role != "OWNER") {
                binding.btnSendInvite.isEnabled = false
                Toast.makeText(requireContext(), "Sadece grup sahibi davet gönderebilir!", Toast.LENGTH_LONG).show()
            } else {
                binding.btnSendInvite.isEnabled = true
            }
        }

        binding.btnSendInvite.setOnClickListener {
            val inviteEmail = binding.etEmail.text.toString().trim()

            if (inviteEmail.isNotEmpty() && groupId.isNotEmpty()) {
                viewModel.inviteUserToGroup(groupId, groupName, inviteEmail)
            } else {
                Toast.makeText(requireContext(), "E-posta adresi boş olamaz!", Toast.LENGTH_SHORT).show()
            }
        }


        viewModel.inviteState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Davetiye başarıyla gönderildi!", Toast.LENGTH_LONG).show()
                    findNavController().navigateUp()
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message ?: "Hata oluştu!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
