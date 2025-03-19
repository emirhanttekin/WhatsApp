package com.example.whatsapp.ui.group

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.whatsapp.R
import com.example.whatsapp.databinding.FragmentGroupDetailsBinding
import com.example.whatsapp.ui.group.adapter.GroupMembersAdapter
import com.example.whatsapp.ui.group.viewmodel.GroupDetailsViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GroupDetailsFragment : Fragment(R.layout.fragment_group_details) {

    private lateinit var binding: FragmentGroupDetailsBinding
    private val viewModel: GroupDetailsViewModel by viewModels()
    private val args: GroupDetailsFragmentArgs by navArgs()

    private lateinit var adapter: GroupMembersAdapter
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentGroupDetailsBinding.bind(view)

        val groupId = args.groupId
        Log.d("GroupDetailsFragment", "📌 Gelen Group ID: $groupId")

        binding.rvGroupMembers.layoutManager = LinearLayoutManager(requireContext())


        viewModel.fetchGroupDetails(groupId)

        viewModel.groupDetailsLiveData.observe(viewLifecycleOwner) { groupDetails ->
            Log.d("GroupDetailsFragment", "📌 Güncellenen Grup Bilgileri: $groupDetails")


            adapter = GroupMembersAdapter(groupDetails.ownerId)
            binding.rvGroupMembers.adapter = adapter


            adapter.submitList(groupDetails.members)
        }
        checkIfUserIsOwner(groupId)
        binding.btnInviteMember.setOnClickListener {
            val action = GroupDetailsFragmentDirections.actionGroupDetailsFragmentToInviteUserFragment(groupId, args.groupName)
            findNavController().navigate(action)
        }
    }


    private fun checkIfUserIsOwner(groupId: String) {
        val currentUserId = auth.currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("groups").document(groupId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val ownerId = document.getString("ownerId") ?: ""
                    Log.d("ChatFragment", "👑 Grup Sahibi ID: $ownerId")


                    if (currentUserId == ownerId) {
                        binding.btnInviteMember.visibility = View.VISIBLE
                    } else {
                        binding.btnInviteMember.visibility = View.GONE
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatFragment", "❌ Grup sahibi bilgisi alınamadı: ${e.message}")
            }
    }
}
