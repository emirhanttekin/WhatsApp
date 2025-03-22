package com.example.whatsapp.ui.group

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.whatsapp.R
import com.example.whatsapp.databinding.FragmentGroupDetailsBinding
import com.example.whatsapp.ui.group.adapter.GroupMembersAdapter
import com.example.whatsapp.ui.group.viewmodel.GroupDetailsViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity
import dagger.hilt.android.AndroidEntryPoint
import java.io.ByteArrayOutputStream
import java.io.File

@AndroidEntryPoint
class GroupDetailsFragment : Fragment(R.layout.fragment_group_details) {

    private lateinit var binding: FragmentGroupDetailsBinding
    private val viewModel: GroupDetailsViewModel by viewModels()
    private val args: GroupDetailsFragmentArgs by navArgs()
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private var isOwner = false
    private var selectedImageUri: Uri? = null

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { startCrop(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentGroupDetailsBinding.bind(view)

        binding.rvGroupMembers.layoutManager = LinearLayoutManager(requireContext())

        setupObservers()
        checkIfUserIsOwner(args.groupId)

        binding.btnInviteMember.setOnClickListener {
            val action = GroupDetailsFragmentDirections
                .actionGroupDetailsFragmentToInviteUserFragment(args.groupId, args.groupName)
            findNavController().navigate(action)
        }

        binding.imgGroupPhoto.setOnClickListener {
            if (isOwner) {
                openGallery()
            }
        }

        viewModel.fetchGroupDetails(args.groupId)
    }

    private fun setupObservers() {
        viewModel.groupDetailsLiveData.observe(viewLifecycleOwner) { groupDetails ->
            binding.tvGroupName.text = args.groupName

            Glide.with(this)
                .load(groupDetails.imageUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_group_placeholder)
                .into(binding.imgGroupPhoto)

            val adapter = GroupMembersAdapter(groupDetails.ownerId)
            binding.rvGroupMembers.adapter = adapter
            adapter.submitList(groupDetails.members)

            // ✅ Online kullanıcıları dinle
            listenToOnlineUsers(args.groupId, adapter)
        }
    }


    private fun checkIfUserIsOwner(groupId: String) {
        val currentUserId = auth.currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("groups").document(groupId)
            .get()
            .addOnSuccessListener { document ->
                val ownerId = document.getString("ownerId") ?: ""
                isOwner = currentUserId == ownerId
                binding.btnInviteMember.visibility = if (isOwner) View.VISIBLE else View.GONE
            }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun startCrop(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(File(requireContext().cacheDir, "cropped_group.jpg"))

        val options = UCrop.Options().apply {
            setCompressionFormat(Bitmap.CompressFormat.PNG)
            setCompressionQuality(100)
            setCircleDimmedLayer(true)
            setHideBottomControls(true)
            setShowCropGrid(false)
            setShowCropFrame(false)
            setFreeStyleCropEnabled(false)
            setAllowedGestures(UCropActivity.SCALE, UCropActivity.NONE, UCropActivity.SCALE)
        }

        UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(1000, 1000)
            .withOptions(options)
            .start(requireContext(), this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == UCrop.REQUEST_CROP && resultCode == Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(data!!)
            resultUri?.let { uploadGroupImage(it) }
        }
    }

    private fun uploadGroupImage(imageUri: Uri) {
        val storageRef = FirebaseStorage.getInstance()
            .reference.child("group_images/${args.groupId}.jpg")

        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    updateGroupImageUrl(uri.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Fotoğraf yüklenemedi!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateGroupImageUrl(imageUrl: String) {
        FirebaseFirestore.getInstance()
            .collection("groups")
            .document(args.groupId)
            .update("imageUrl", imageUrl)
            .addOnSuccessListener {
                Glide.with(this)
                    .load(imageUrl)
                    .circleCrop()
                    .into(binding.imgGroupPhoto)
                Toast.makeText(requireContext(), "Grup fotoğrafı güncellendi!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun listenToOnlineUsers(groupId: String, adapter: GroupMembersAdapter) {
        val onlineRef = FirebaseFirestore.getInstance()
            .collection("groups")
            .document(groupId)
            .collection("onlineUsers")

        onlineRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("GroupDetails", "❌ Online kullanıcıları dinlerken hata: ${error.message}")
                return@addSnapshotListener
            }

            val onlineUserIds = snapshot?.documents?.mapNotNull { it.id } ?: emptyList()
            adapter.updateOnlineUsers(onlineUserIds)

            Log.d("GroupDetails", "✅ Online kullanıcılar: $onlineUserIds")
        }
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

