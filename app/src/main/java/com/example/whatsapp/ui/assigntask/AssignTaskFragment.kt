package com.example.whatsapp.ui.assigntask

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.whatsapp.R
import com.example.whatsapp.data.model.AssignedTask
import com.example.whatsapp.databinding.FragmentAssignTaskBinding
import com.example.whatsapp.ui.assigntask.adapter.AssignedTaskAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AssignTaskFragment : Fragment(R.layout.fragment_assign_task) {

    private lateinit var binding: FragmentAssignTaskBinding
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    @Inject
    lateinit var assignedTaskAdapter: AssignedTaskAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAssignTaskBinding.bind(view)

        setupRecyclerView()
        loadAssignedTasks()
    }

    private fun setupRecyclerView() {
        binding.rvAssignedTasks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = assignedTaskAdapter
        }
    }

    private fun loadAssignedTasks() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collectionGroup("assignedTasks")
            .whereEqualTo("assigneeId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val tasks = snapshot.documents.mapNotNull { it.toObject(AssignedTask::class.java) }
                Log.d("🔥 AssignTaskFragment", "Toplam görev: ${tasks.size}")
                tasks.forEach { Log.d("🔥 AssignTaskFragment", "Görev: ${it.messageText}") }

                assignedTaskAdapter.submitList(tasks)
            }
            .addOnFailureListener {
                Log.e("🔥 AssignTaskFragment", "Görevler alınamadı: ${it.message}")
                Toast.makeText(requireContext(), "Görevler alınamadı", Toast.LENGTH_SHORT).show()
            }
    }

}
