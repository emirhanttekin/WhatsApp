package com.example.whatsapp.ui.chat

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.whatsapp.R
import com.example.whatsapp.data.model.Message
import com.example.whatsapp.databinding.FragmentChatBinding
import com.example.whatsapp.ui.assigntask.AssignTaskBottomSheet
import com.example.whatsapp.ui.chat.adapter.ChatAdapter
import com.example.whatsapp.ui.chat.viewmodel.ChatViewModel
import com.example.whatsapp.utils.helper.ImageLoader
import com.example.whatsapp.utils.helper.MessageOptionHelper
import com.example.whatsapp.utils.helper.PermissionHelper
import com.example.whatsapp.utils.sender.MessageSender
import com.example.whatsapp.utils.upload.UploadManager
import com.example.whatsapp.utils.voice.VoiceRecorderManager
import com.example.whatsapp.utils.voice.VoiceRecordingController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.vanniktech.emoji.EmojiPopup


import dagger.hilt.android.AndroidEntryPoint
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class ChatFragment : Fragment(R.layout.fragment_chat) {

    private lateinit var binding: FragmentChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private val args: ChatFragmentArgs by navArgs()
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private lateinit var voiceRecorderManager: VoiceRecorderManager
    private lateinit var voiceController: VoiceRecordingController


    @Inject
    lateinit var chatAdapter: ChatAdapter

    private val REQUEST_CAMERA_PERMISSION = 1001
    private var imageUri: Uri? = null
    private val documentPickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                uploadDocumentToFirebase(it)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentChatBinding.bind(view)
        setupEmojiPopup()
        val groupId = args.groupId
        val groupName = args.groupName
        voiceRecorderManager = VoiceRecorderManager(requireContext())
        voiceController = VoiceRecordingController(
            context = requireContext(),
            voiceRecorderManager = voiceRecorderManager,
            recorderLayout = binding.voiceRecorderLayout,
            timerView = binding.tvRecordingTimer,
            onRecordingFinished = { audioPath ->
                uploadAudioToFirebase(audioPath)
            }
        )


        setupVoiceRecording()


        setupSendButtonVisibility()

        Log.d("ChatFragment", "✅ ChatFragment → Group ID: $groupId")
        binding.btnAttachment.setOnClickListener {
            openDocumentPicker()
        }

        binding.tvGroupName.text = groupName
        setupRecyclerView()
        checkIfUserIsOwner(groupId)
        markMessagesAsRead(groupId)
        binding.btnInvite.setOnClickListener {
            val action =
                ChatFragmentDirections.actionChatFragmentToInviteUserFragment(groupId, groupName)
            findNavController().navigate(action)
        }

        binding.tvGroupName.setOnClickListener {
            val action = ChatFragmentDirections.actionChatFragmentToGroupDetailsFragment(
                groupId = groupId,
                groupName = groupName
            )
            findNavController().navigate(action)

        }

        loadGroupProfileImage(groupId)

        viewModel.connectSocket()

        auth.currentUser?.uid?.let { userId ->
            viewModel.joinGroup(userId, groupId)
        }

        listenToOnlineUsers(args.groupId)

        viewModel.messagesLiveData.observe(viewLifecycleOwner) { messages ->
            chatAdapter.submitList(ArrayList(messages)) {
                binding.rvMessages.post {
                    if (messages.isNotEmpty()) {
                        binding.rvMessages.scrollToPosition(messages.size - 1)
                    }
                }
            }
        }


        binding.btnCamera.setOnClickListener {
            showImagePickerDialog()
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

    }


    private fun openDocumentPicker() {
        documentPickerLauncher.launch("*/*")
    }




    private fun setupEmojiPopup() {
        val emojiPopup = EmojiPopup(binding.root, binding.etMessage)

        binding.btnEmoji.setOnClickListener {
            emojiPopup.toggle()
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
    private fun uploadDocumentToFirebase(fileUri: Uri) {
        UploadManager.uploadDocument(
            context = requireContext(),
            fileUri = fileUri,
            onSuccess = { fileUrl ->
                MessageSender.sendFileMessage(viewModel, args.groupId, fileUrl)
                Toast.makeText(requireContext(), "📁 Belge gönderildi", Toast.LENGTH_SHORT).show()
            },
            onFailure = {
                Toast.makeText(requireContext(), "❌ Belge yüklenemedi", Toast.LENGTH_SHORT).show()
            }
        )
    }
    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // İzin verildiyse, kayıt başlat
            voiceController.startRecordingManually()
        } else {
            Toast.makeText(requireContext(), "Mikrofon izni reddedildi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupVoiceRecording() {
        binding.btnMic.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (PermissionHelper.isPermissionGranted(requireContext(), Manifest.permission.RECORD_AUDIO)) {
                        voiceController.startRecordingManually()
                    } else {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    voiceController.stopRecordingManually()
                    true
                }

                else -> false
            }
        }
    }

    private fun setupSendButtonVisibility() {
        binding.etMessage.addTextChangedListener {
            val text = it?.toString()?.trim()

            if (text.isNullOrEmpty()) {

                binding.btnMic.setImageResource(R.drawable.ic_mic)
                binding.btnMic.contentDescription = "Sesli Mesaj"

                setupVoiceRecording()

            } else {

                binding.btnMic.setImageResource(R.drawable.ic_send)
                binding.btnMic.contentDescription = "Mesaj Gönder"

                binding.btnMic.setOnTouchListener(null)


                binding.btnMic.setOnClickListener {
                    val message = binding.etMessage.text.toString().trim()
                    if (message.isNotEmpty()) {
                        MessageSender.sendTextMessage(viewModel, args.groupId, message)
                        binding.etMessage.text?.clear()
                        setupVoiceRecording()
                    }
                }
            }
        }
    }

    private fun uploadAudioToFirebase(filePath: String) {
        val audioUri = Uri.fromFile(File(filePath))
        UploadManager.uploadAudio(
            context = requireContext(),
            fileUri = audioUri,
            onSuccess = { audioUrl ->
                MessageSender.sendAudioMessage(viewModel, args.groupId, audioUrl)
            },
            onFailure = {
                Toast.makeText(requireContext(), "❌ Ses yüklenemedi", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun loadGroupProfileImage(groupId: String) {
        val groupRef = FirebaseFirestore.getInstance().collection("groups").document(groupId)

        groupRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val imageUrl = document.getString("imageUrl")
                ImageLoader.loadCircleImage(
                    context = requireContext(),
                    url = imageUrl,
                    imageView = binding.imgGroupProfile,
                    placeholder = R.drawable.ic_group_placeholder
                )
            }
        }.addOnFailureListener {
            Log.e("ChatFragment", "Grup resmi alınamadı: ${it.message}")
        }
    }


    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            bitmap?.let { uploadImageToFirebase(convertBitmapToUri(it)) }
        }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(android.Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
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
        UploadManager.uploadImage(
            context = requireContext(),
            imageUri = imageUri,
            onSuccess = { imageUrl ->
                MessageSender.sendImageMessage(viewModel, args.groupId, imageUrl)
            },
            onFailure = {}
        )
    }


    private fun convertBitmapToUri(bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(
            requireContext().contentResolver,
            bitmap,
            "Title",
            null
        )
        return Uri.parse(path)
    }

    private fun setupRecyclerView() {
        binding.rvMessages.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
        }

        chatAdapter.onMessageLongClick = { message ->
            MessageOptionHelper.showOptions(requireContext(), message, args.groupId, childFragmentManager)
        }

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


        if (viewModel.messagesLiveData.value.isNullOrEmpty()) {

            viewModel.loadMessagesFromFirestore(args.groupId)
        }

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
        markUserOffline(args.groupId)
    }

    private fun listenToOnlineUsers(groupId: String) {
        val ref = FirebaseFirestore.getInstance()
            .collection("groups")
            .document(groupId)
            .collection("onlineUsers")

        ref.addSnapshotListener { snapshot, error ->
            val onlineCount = snapshot?.size() ?: 0
            binding.tvGroupInfo.text = "$onlineCount kişi • Online"
        }

    }

    private fun markUserOffline(groupId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("groups")
            .document(groupId)
            .collection("onlineUsers")
            .document(userId)
            .delete()
    }



}
