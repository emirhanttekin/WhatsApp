package com.example.whatsapp.ui.chat

import android.app.AlertDialog
import android.content.pm.PackageManager
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
import com.example.whatsapp.R
import com.example.whatsapp.databinding.FragmentChatBinding
import com.example.whatsapp.ui.chat.adapter.ChatAdapter
import com.example.whatsapp.ui.chat.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.AndroidEntryPoint
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class ChatFragment : Fragment(R.layout.fragment_chat) {

    private lateinit var binding: FragmentChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private val args: ChatFragmentArgs by navArgs()
    private var isChatScreenVisible: Boolean = false
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    @Inject
    lateinit var chatAdapter: ChatAdapter

    private val REQUEST_CAMERA_PERMISSION = 1001
    private var imageUri: Uri? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentChatBinding.bind(view)

        val groupId = args.groupId
        val groupName = args.groupName

        Log.d("ChatFragment", "✅ ChatFragment → Group ID: $groupId")

        binding.tvGroupName.text = groupName
        setupRecyclerView()
        checkIfUserIsOwner(groupId)
        markMessagesAsRead(groupId)
        binding.btnInvite.setOnClickListener {
            val action = ChatFragmentDirections.actionChatFragmentToInviteUserFragment(groupId, groupName)
            findNavController().navigate(action)
        }

        binding.tvGroupName.setOnClickListener {
            val action = ChatFragmentDirections.actionChatFragmentToGroupDetailsFragment(
                groupId = groupId,
                groupName = groupName)
            findNavController().navigate(action)

        }


        viewModel.connectSocket()

        auth.currentUser?.uid?.let { userId ->
            viewModel.joinGroup(userId, groupId)
        }


        viewModel.messagesLiveData.observe(viewLifecycleOwner) { messages ->
            chatAdapter.submitList(ArrayList(messages)) {
                binding.rvMessages.postDelayed({
                    if (messages.isNotEmpty()) {
                        binding.rvMessages.smoothScrollToPosition(messages.size - 1)
                    }
                }, 200)
                chatAdapter.notifyDataSetChanged()
            }
        }


        binding.btnSend.setOnClickListener {
            val messageText = binding.etMessage.text.toString().trim()
            sendMessage(args.groupId, messageText)
        }

        binding.btnCamera.setOnClickListener {
            showImagePickerDialog()
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

    }

    private fun showImagePickerDialog() {
        val options = arrayOf("📷 Fotoğraf Çek", "🖼️ Galeriden Seç", "❌ İptal")

        AlertDialog.Builder(requireContext())
            .setTitle("Fotoğraf Ekle")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
            .show()
    }


    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            bitmap?.let { uploadImageToFirebase(convertBitmapToUri(it)) }
        }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        } else {
            cameraLauncher.launch(null)
        }
    }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { uploadImageToFirebase(it) }
        }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun uploadImageToFirebase(imageUri: Uri) {
        val storageRef = FirebaseStorage.getInstance().reference.child("chat_images/${System.currentTimeMillis()}.jpg")

        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    val groupId = args.groupId
                    sendMessage(groupId, imageUrl = downloadUrl.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Fotoğraf yüklenemedi!", Toast.LENGTH_SHORT).show()
            }
    }



    private fun convertBitmapToUri(bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(requireContext().contentResolver, bitmap, "Title", null)
        return Uri.parse(path)
    }

    private fun setupRecyclerView() {
        binding.rvMessages.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
        }
    }

    private fun sendMessage(groupId: String, messageText: String? = null, imageUrl: String? = null) {
        if (messageText.isNullOrEmpty() && imageUrl.isNullOrEmpty()) {
            Log.e("ChatFragment", "Gönderilecek mesaj veya resim yok!")
            return
        }

        auth.currentUser?.uid?.let { userId ->
            viewModel.sendMessage(groupId, messageText, imageUrl, userId)
        }

        binding.etMessage.text?.clear()
    }


    private fun checkIfUserIsOwner(groupId: String) {
        val currentUserId = auth.currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("groups").document(groupId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val ownerId = document.getString("ownerId") ?: ""
                    if (currentUserId == ownerId) {
                        binding.btnInvite.visibility = View.VISIBLE
                    } else {
                        binding.btnInvite.visibility = View.GONE
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatFragment", " Grup sahibi bilgisi alınamadı: ${e.message}")
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.disconnectSocket()
    }

    override fun onResume() {
        super.onResume()
        viewModel.connectSocket()
        viewModel.loadMessagesFromRoom(args.groupId)
        viewModel.loadMessagesFromFirestore(args.groupId)
        viewModel.isChatScreenVisible = true
    }
    fun markMessagesAsRead(groupId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val groupRef = FirebaseFirestore.getInstance().collection("groups").document(groupId)
        groupRef.update("unreadMessages.$userId", 0)
    }


    override fun onPause() {
        super.onPause()
        viewModel.isChatScreenVisible = false
    }
}
