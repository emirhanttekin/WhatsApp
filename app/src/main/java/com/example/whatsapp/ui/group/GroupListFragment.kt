package com.example.whatsapp.ui.group

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.whatsapp.R
import com.example.whatsapp.databinding.FragmentGroupListBinding
import com.example.whatsapp.ui.group.adapter.GroupAdapter
import com.example.whatsapp.ui.group.viewmodel.GroupListViewModel
import com.example.whatsapp.utils.Resource
import com.google.firebase.auth.FirebaseAuth
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

        viewModel.groupList.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Loading -> binding.progressBar.visibility = View.VISIBLE
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    groupListAdapter.submitList(state.data ?: emptyList()) // ✅ NULL KONTROLÜ
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Gruplar yüklenemedi!", Toast.LENGTH_LONG).show()
                }
            }
        }


        binding.btnProfile.setOnClickListener {
            findNavController().navigate(R.id.action_groupListFragment_to_profileFragment)
        }

        // Çıkış Yap Butonu
        binding.btnLogout.setOnClickListener {
            logoutUser()
        }

        binding.fabCreateGroup.setOnClickListener {
            findNavController().navigate(R.id.action_groupListFragment_to_createGroupFragment)
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

    private fun logoutUser() {
        // Firebase Authentication'dan çıkış yap
        auth.signOut()

        // Kullanıcı bilgisini temizle
        clearUserSession()

        // Login sayfasına yönlendir
        findNavController().navigate(R.id.action_groupListFragment_to_loginFragment)
    }

    private fun clearUserSession() {
        // 🔥 SharedPreferences Kullanarak Kullanıcı Oturumunu Temizle
        val sharedPreferences = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()

        // 🔥 Firestore Cache Temizleme
        firestore.clearPersistence()
    }
}
