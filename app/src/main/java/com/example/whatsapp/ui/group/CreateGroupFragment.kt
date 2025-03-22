package com.example.whatsapp.ui.group

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.whatsapp.R
import com.example.whatsapp.databinding.FragmentCreateGroupBinding
import com.example.whatsapp.ui.company.viewmodel.CreateGroupViewModel
import com.example.whatsapp.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class CreateGroupFragment : Fragment(R.layout.fragment_create_group) {

    private val viewModel: CreateGroupViewModel by viewModels()
    private lateinit var binding: FragmentCreateGroupBinding
    private var companyId: String = ""
    private var selectedGroupImageUri: Uri? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentCreateGroupBinding.bind(view)

        // Şirket ID al
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    companyId = document.getString("companyId") ?: ""
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Şirket ID alınamadı!", Toast.LENGTH_SHORT).show()
            }

        // Grup resmi seçme
        binding.btnSelectGroupImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 1001)
        }

        // Grup oluştur
        binding.btnCreateGroup.setOnClickListener {
            val groupName = binding.etGroupName.text.toString().trim()
            if (groupName.isNotEmpty() && companyId.isNotEmpty()) {
                if (selectedGroupImageUri != null) {
                    val groupId = FirebaseFirestore.getInstance().collection("groups").document().id
                    val storageRef = FirebaseStorage.getInstance().reference.child("group_images/$groupId.jpg")

                    storageRef.putFile(selectedGroupImageUri!!)
                        .addOnSuccessListener {
                            storageRef.downloadUrl.addOnSuccessListener { uri ->
                                viewModel.createGroup(companyId, groupName, uri.toString())
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Grup resmi yüklenemedi!", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    viewModel.createGroup(companyId, groupName, "")
                }
            } else {
                Toast.makeText(requireContext(), "Grup adı veya şirket ID eksik!", Toast.LENGTH_SHORT).show()
            }
        }

        // ViewModel durumu gözle
        viewModel.createGroupState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnCreateGroup.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnCreateGroup.isEnabled = true
                    Toast.makeText(requireContext(), "Grup başarıyla oluşturuldu!", Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.action_createGroupFragment_to_groupListFragment)
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnCreateGroup.isEnabled = true
                    Toast.makeText(requireContext(), state.message ?: "Grup oluşturulamadı!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            val sourceUri = data?.data ?: return
            startCrop(sourceUri)
        } else if (requestCode == UCrop.REQUEST_CROP && resultCode == Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(data!!)
            selectedGroupImageUri = resultUri

            Glide.with(this)
                .load(resultUri)
                .circleCrop()
                .placeholder(R.drawable.ic_group_placeholder)
                .into(binding.imgGroupImage)
        }
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
}
