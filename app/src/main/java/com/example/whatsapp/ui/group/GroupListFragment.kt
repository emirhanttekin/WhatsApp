package com.example.whatsapp.ui.group

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.whatsapp.R
import com.example.whatsapp.databinding.FragmentGroupListBinding
import com.example.whatsapp.ui.group.adapter.GroupAdapter
import com.example.whatsapp.ui.group.viewmodel.GroupListViewModel
import com.example.whatsapp.utils.Resource
import com.example.whatsapp.utils.helper.PermissionHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GroupListFragment : Fragment(R.layout.fragment_group_list) {

    private val viewModel: GroupListViewModel by viewModels()
    private lateinit var binding: FragmentGroupListBinding
    private lateinit var groupListAdapter: GroupAdapter
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentGroupListBinding.bind(view)

        setupRecyclerView()
        viewModel.loadGroups()
        viewModel.checkForPendingInvites()
        checkForGroupInvitations()
        binding.etSearchGroup.addTextChangedListener { text ->
            val query = text.toString().trim()
            viewModel.filterGroups(query)
        }

        viewModel.filteredGroups.observe(viewLifecycleOwner) { groups ->
            groupListAdapter.submitList(groups)
        }


        viewModel.groupList.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Loading -> binding.progressBar.visibility = View.VISIBLE
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    groupListAdapter.submitList(state.data ?: emptyList())
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Gruplar yüklenemedi!", Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.btnTask.setOnClickListener {
            findNavController().navigate(R.id.action_groupListFragment_to_assignTaskFragment)
        }


        binding.btnProfile.setOnClickListener {
            findNavController().navigate(R.id.action_groupListFragment_to_profileFragment)
        }


        binding.btnLogout.setOnClickListener {
            logoutUser()
        }

        binding.fabCreateGroup.setOnClickListener {
            findNavController().navigate(R.id.action_groupListFragment_to_createGroupFragment)
        }
    }
    private val microphonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(requireContext(), "Mikrofon izni verildi 🎉", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Mikrofon izni reddedildi ❌", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        groupListAdapter = GroupAdapter(mutableListOf()) { group ->
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            val isOwner = group.ownerId == currentUserId
            val groupInfo = if (isOwner) "${group.name} (Sahibi)" else group.name

            val action = GroupListFragmentDirections
                .actionGroupListFragmentToChatFragment(groupId = group.id, groupInfo)
            findNavController().navigate(action)

        }

        binding.rvGroups.apply {
            adapter = groupListAdapter
            setHasFixedSize(true)
        }
    }
    private fun checkForGroupInvitations() {
        val user = auth.currentUser ?: return
        val userEmail = user.email ?: return

        firestore.collection("groupInvitations")
            .document(userEmail)
            .get()
            .addOnSuccessListener { inviteDocument ->
                if (inviteDocument.exists()) {
                    val groupId = inviteDocument.getString("groupId") ?: return@addOnSuccessListener
                    val groupName = inviteDocument.getString("groupName") ?: "Bilinmeyen Grup"

                    AlertDialog.Builder(requireContext())
                        .setTitle("Grup Daveti")
                        .setMessage("$groupName adlı gruba katılmak istiyor musunuz?")
                        .setPositiveButton("Kabul Et") { _, _ ->
                            acceptInvitation(user.uid, userEmail, groupId)
                        }
                        .setNegativeButton("Reddet") { _, _ ->
                            declineInvitation(userEmail)
                        }
                        .show()
                }
            }
    }


    private fun acceptInvitation(userId: String, userEmail: String, groupId: String) {
        firestore.collection("groups").document(groupId)
            .update("members", FieldValue.arrayUnion(userId))
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Gruba başarıyla katıldınız!", Toast.LENGTH_SHORT).show()
                deleteInvitation(userEmail)
                viewModel.loadGroups() // Listeyi güncelle
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gruba katılırken hata oluştu!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun declineInvitation(userEmail: String) {
        deleteInvitation(userEmail)
        Toast.makeText(requireContext(), "Davet reddedildi", Toast.LENGTH_SHORT).show()
    }

    private fun deleteInvitation(userEmail: String) {
        firestore.collection("groupInvitations").document(userEmail)
            .delete()
            .addOnSuccessListener {
                // Davet silindi
            }
    }
    private fun checkMicrophonePermission() {
        if (!PermissionHelper.isPermissionGranted(requireContext(), Manifest.permission.RECORD_AUDIO)) {
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun logoutUser() {

        auth.signOut()


        clearUserSession()


        findNavController().navigate(R.id.action_groupListFragment_to_loginFragment)
    }

    private fun clearUserSession() {

        val sharedPreferences = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()


        firestore.clearPersistence()
    }

    override fun onResume() {
        super.onResume()
        markUserOnlineInFirestore()
    }


    private fun markUserOnlineInFirestore() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("groups")
            .whereArrayContains("members", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { doc ->
                    val groupId = doc.id
                    val onlineRef = FirebaseFirestore.getInstance()
                        .collection("groups")
                        .document(groupId)
                        .collection("onlineUsers")
                        .document(userId)

                    // Kullanıcıyı online olarak işaretle
                    onlineRef.set(mapOf("status" to true))
                }
            }
    }


}
