package com.example.whatsapp.ui.assigntask

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.whatsapp.data.model.AssignedTask
import com.example.whatsapp.data.model.Message
import com.example.whatsapp.data.model.User
import com.example.whatsapp.databinding.BottomsheetAssignTaskBinding
import com.example.whatsapp.ui.assigntask.adapter.AssigneeAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AssignTaskBottomSheet(
    private val message: Message,
    private val groupId: String
) : BottomSheetDialogFragment() {

    private var _binding: BottomsheetAssignTaskBinding? = null
    private val binding get() = _binding!!

    private lateinit var assignAdapter: AssigneeAdapter
    private var selectedDateTime: Calendar = Calendar.getInstance()
    private var selectedAssigneeId: String? = null

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetAssignTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupMessagePreview()
        setupRecyclerView()
        loadGroupMembers()

        binding.btnSelectDateTime.setOnClickListener {
            showDateTimePicker()
        }

        binding.btnSave.setOnClickListener {
            val selectedAssigneeIds = assignAdapter.getSelectedUserIds()
            if (selectedAssigneeIds.isEmpty()) {
                Toast.makeText(requireContext(), "Lütfen görev atanacak kişileri seçin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveTaskToFirestore(selectedAssigneeIds)
        }


    }

    private fun setupMessagePreview() {
        // Hepsini gizle
        binding.etMessageContent.visibility = View.GONE
        binding.imgMessagePreview.visibility = View.GONE
        binding.audioLayout.visibility = View.GONE
        binding.fileLayout.visibility = View.GONE

        when {
            !message.imageUrl.isNullOrEmpty() -> {
                binding.imgMessagePreview.visibility = View.VISIBLE
                Glide.with(requireContext())
                    .load(message.imageUrl)
                    .into(binding.imgMessagePreview)
            }
            !message.audioUrl.isNullOrEmpty() -> {
                binding.audioLayout.visibility = View.VISIBLE
            }
            !message.fileUrl.isNullOrEmpty() -> {
                binding.fileLayout.visibility = View.VISIBLE
                binding.tvFileName.text = extractFileName(message.fileUrl!!)
            }
            !message.message.isNullOrEmpty() -> {
                binding.etMessageContent.visibility = View.VISIBLE
                binding.etMessageContent.setText(message.message)
            }
        }
    }

    private fun setupRecyclerView() {
        assignAdapter = AssigneeAdapter(
            onUserClick = { user ->

            }
        )

        binding.rvAssignees.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = assignAdapter
        }
    }

    private fun loadGroupMembers() {
        firestore.collection("groups")
            .document(groupId)
            .get()
            .addOnSuccessListener { document ->
                val memberIds = document.get("members") as? List<String> ?: return@addOnSuccessListener
                fetchMemberProfiles(memberIds)
            }
            .addOnFailureListener {
                Log.e("AssignTask", "❌ Grup verisi alınamadı: ${it.message}")
            }
    }

    private fun fetchMemberProfiles(userIds: List<String>) {
        firestore.collection("users")
            .whereIn(FieldPath.documentId(), userIds)
            .get()
            .addOnSuccessListener { snapshot ->
                val users = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(uid = doc.id)
                }
                assignAdapter.submitList(users)
            }
            .addOnFailureListener {
                Log.e("AssignTask", "❌ Kullanıcılar alınamadı: ${it.message}")
            }
    }

    private fun showDateTimePicker() {
        val now = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            TimePickerDialog(requireContext(), { _, hour, minute ->
                selectedDateTime.set(year, month, day, hour, minute)
                val format = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
                binding.tvSelectedDateTime.text = format.format(selectedDateTime.time)
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun saveTaskToFirestore(assigneeIds: List<String>) {
        val currentUserId = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(currentUserId)
            .get()
            .addOnSuccessListener { assignerSnapshot ->
                val assignerName = assignerSnapshot.getString("name") ?: "Bilinmiyor"
                val assignerProfileUrl = assignerSnapshot.getString("profileImageUrl")


                firestore.collection("users")
                    .whereIn(FieldPath.documentId(), assigneeIds)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val assigneeList = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(User::class.java)?.copy(uid = doc.id)
                        }

                        assigneeList.forEach { user ->
                            val newTaskId = UUID.randomUUID().toString()

                            val task = AssignedTask(
                                id = newTaskId,
                                messageId = message.id,
                                messageText = when {
                                    !message.audioUrl.isNullOrEmpty() -> "[Sesli mesaj]"
                                    !message.imageUrl.isNullOrEmpty() -> "[Görsel mesaj]"
                                    !message.fileUrl.isNullOrEmpty() -> extractFileName(message.fileUrl!!)
                                    !message.message.isNullOrEmpty() -> message.message
                                    else -> "Görev içeriği"
                                },
                                audioUrl = message.audioUrl,
                                imageUrl = message.imageUrl,
                                fileUrl = message.fileUrl,
                                assignerId = currentUserId,
                                assignerName = assignerName,
                                assignerProfileUrl = assignerProfileUrl,
                                assigneeId = user.uid,
                                assigneeName = user.name,
                                assigneeProfileUrl = user.profileImageUrl,
                                assignees = assigneeList, // ✅ burada tüm kullanıcılar set ediliyor
                                groupId = groupId,
                                deadline = selectedDateTime.timeInMillis
                            )

                            firestore.collection("groups")
                                .document(groupId)
                                .collection("assignedTasks")
                                .document(newTaskId)
                                .set(task)
                        }

                        Toast.makeText(requireContext(), "✅ Görev(ler) başarıyla atandı!", Toast.LENGTH_SHORT).show()
                        dismiss()
                    }
            }
    }





    private fun extractFileName(url: String): String {
        return Uri.parse(url).lastPathSegment ?: "dosya"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
