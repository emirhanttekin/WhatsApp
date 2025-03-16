package com.example.whatsapp.ui.auth

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.whatsapp.R
import com.example.whatsapp.databinding.FragmentProfileSetupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class ProfileSetupFragment : Fragment(R.layout.fragment_profile_setup) {

    private lateinit var binding: FragmentProfileSetupBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    private var selectedImageUri: Uri? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentProfileSetupBinding.bind(view)

        binding.btnSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 1001)
        }

        binding.btnSaveProfile.setOnClickListener {
            saveUserProfile()
            findNavController().navigate(R.id.action_profileSetupFragment2_to_createCompanyFragment)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            val sourceUri = data?.data ?: return
            startCrop(sourceUri)
        } else if (requestCode == UCrop.REQUEST_CROP && resultCode == Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(data!!)
            selectedImageUri = resultUri


            Glide.with(this)
                .load(resultUri)
                .circleCrop()
                .placeholder(R.drawable.ic_profile_placeholder)
                .into(binding.imgProfile)
        }
    }






    private fun saveUserProfile() {
        val name = binding.etName.text.toString().trim()
        val surname = binding.etSurname.text.toString().trim()

        if (name.isEmpty() || surname.isEmpty()) {
            Toast.makeText(requireContext(), "Lütfen isim ve soyisim girin", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return
        if (selectedImageUri != null) {
            val storageRef = storage.reference.child("profile_images/$userId.jpg")
            storageRef.putFile(selectedImageUri!!)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        saveToFirestore(userId, name, surname, uri.toString())
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Resim yüklenemedi!", Toast.LENGTH_SHORT).show()
                }
        } else {
            saveToFirestore(userId, name, surname, "")
        }
    }

    private fun startCrop(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(File(requireContext().cacheDir, "cropped_image.jpg"))

        val options = UCrop.Options().apply {
            setCompressionFormat(Bitmap.CompressFormat.PNG)  // Şeffaflık desteği için PNG
            setCompressionQuality(100)  // En yüksek kalite
            setHideBottomControls(true) // ✅ Alt kontrolleri (scale vb.) gizle, tam ekran mod
            setCircleDimmedLayer(true)  // ✅ Oval kırpma
            setShowCropGrid(false)  // ✅ Izgarayı kaldır
            setShowCropFrame(false)  // ✅ Kırpma çerçevesini kaldır
            setFreeStyleCropEnabled(false)
            setAllowedGestures(UCropActivity.SCALE, UCropActivity.NONE, UCropActivity.SCALE) // ✅ Sadece yakınlaştırma ve sürükleme
        }

        UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f)  // 1:1 oranında kırp
            .withMaxResultSize(1000, 1000) // Daha yüksek çözünürlük
            .withOptions(options)
            .start(requireContext(), this)
    }


    private fun saveToFirestore(userId: String, name: String, surname: String, profileImageUrl: String) {
        val userUpdate = mapOf(
            "name" to name,
            "surname" to surname,
            "profileImageUrl" to profileImageUrl
        )

        firestore.collection("users").document(userId)
            .set(userUpdate, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profiliniz güncellendi!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Profil güncellenemedi!", Toast.LENGTH_SHORT).show()
            }
    }
}
