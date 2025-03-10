package com.example.whatsapp.ui.group

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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GroupListFragment : Fragment(R.layout.fragment_group_list) {

    private val viewModel: GroupListViewModel by viewModels()
    private lateinit var binding: FragmentGroupListBinding
    private lateinit var groupListAdapter: GroupAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentGroupListBinding.bind(view)

        setupRecyclerView()

        viewModel.loadGroups()
        viewModel.checkForPendingInvites()

        viewModel.groupList.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    groupListAdapter.submitList(state.data ?: emptyList()) // ✅ NULL KONTROLÜ EKLENDİ!
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Gruplar yüklenemedi!", Toast.LENGTH_LONG).show()
                }
            }
        }


        binding.fabCreateGroup.setOnClickListener {
            findNavController().navigate(R.id.action_groupListFragment_to_createGroupFragment)
        }
    }


    private fun setupRecyclerView() {
        groupListAdapter = GroupAdapter(mutableListOf()) { group ->
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

            val isOwner = group.ownerId == currentUserId // ✅ Kullanıcı owner mı?
            val groupInfo = if (isOwner) "${group.name} (Sahibi)" else group.name // ✅ Eğer owner ise etiketi ekle

            val action = GroupListFragmentDirections
                .actionGroupListFragmentToChatFragment(groupId = group.id, groupInfo)
            findNavController().navigate(action)
        }


        binding.rvGroups.apply {
            adapter = groupListAdapter
            setHasFixedSize(true)
        }
    }






}
