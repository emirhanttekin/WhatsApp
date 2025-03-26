package com.example.whatsapp.ui.chat

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
import com.example.whatsapp.databinding.FragmentChatBinding
import com.example.whatsapp.ui.chat.adapter.ChatAdapter
import com.example.whatsapp.ui.chat.viewmodel.ChatViewModel
import com.example.whatsapp.utils.voice.VoiceRecorderManager
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
    private var isChatScreenVisible: Boolean = false
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private lateinit var voiceRecorderManager: VoiceRecorderManager
    private var isRecording = false
    private var recordingStartX = 0f


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

        val groupId = args.groupId
        val groupName = args.groupName
        voiceRecorderManager = VoiceRecorderManager(requireContext())

        setupVoiceRecording()
        setupEmojiPopup()
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
        documentPickerLauncher.launch("*/*") // tüm dosya türlerini destekler
    }


    private fun setupEmojiPopup() {
        val emojiPopup = EmojiPopup(
            rootView = binding.root,
            editText = binding.etMessage
        )

        // Emoji toggle
        binding.btnEmoji.setOnClickListener {
            emojiPopup.toggle()
        }
    }

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startVoiceRecording()
        } else {
            Toast.makeText(requireContext(), "Mikrofon izni reddedildi!", Toast.LENGTH_SHORT).show()
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
        val fileName = System.currentTimeMillis().toString()
        val fileRef = FirebaseStorage.getInstance().reference
            .child("chat_files/$fileName")

        fileRef.putFile(fileUri)
            .addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    sendMessage(args.groupId, fileUrl = downloadUrl.toString())
                    Toast.makeText(requireContext(), "📁 Belge gönderildi", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "❌ Belge yüklenemedi", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupVoiceRecording() {
        val micButton = binding.btnMic

        micButton.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    recordingStartX = motionEvent.x
                    startVoiceRecording()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val diffX = motionEvent.x - recordingStartX
                    if (diffX < -200) { // sola kaydırma
                        cancelVoiceRecording()
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isRecording) {
                        stopVoiceRecording()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun startVoiceRecording() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            return
        }

        val audioPath = voiceRecorderManager.startRecording()
        if (audioPath != null) {
            isRecording = true
            binding.voiceRecorderLayout.visibility = View.VISIBLE
            binding.tvRecordingTimer.base = SystemClock.elapsedRealtime()
            binding.tvRecordingTimer.start()
            Log.d("ChatFragment", "🎙️ Ses kaydı başladı...")
        } else {
            Toast.makeText(requireContext(), "Kayıt başlatılamadı!", Toast.LENGTH_SHORT).show()
        }
    }


    private fun stopVoiceRecording() {
        val audioPath = voiceRecorderManager.stopRecording()
        isRecording = false
        binding.voiceRecorderLayout.visibility = View.GONE
        binding.tvRecordingTimer.stop()

        audioPath?.let {
            uploadAudioToFirebase(it)
        }
    }

    private fun setupSendButtonVisibility() {
        binding.etMessage.addTextChangedListener {
            val text = it?.toString()?.trim()

            if (text.isNullOrEmpty()) {
                // Yazı yoksa: Mikrofonu göster
                binding.btnMic.setImageResource(R.drawable.ic_mic)
                binding.btnMic.contentDescription = "Sesli Mesaj"

                // Touch listener'ı tekrar ekle (basılı tutarak kayıt için)
                setupVoiceRecording()

            } else {
                // Yazı varsa: Gönder ikonunu göster
                binding.btnMic.setImageResource(R.drawable.ic_send)
                binding.btnMic.contentDescription = "Mesaj Gönder"

                // Ses kaydı için olan touch listener'ı devre dışı bırak
                binding.btnMic.setOnTouchListener(null)

                // Bu durumda normal tıklamayla mesaj gönder
                binding.btnMic.setOnClickListener {
                    val messageText = binding.etMessage.text.toString().trim()
                    if (messageText.isNotEmpty()) {
                        sendMessage(args.groupId, messageText)
                    }
                }
            }
        }
    }


    private fun uploadAudioToFirebase(filePath: String) {
        val audioFile = File(filePath)
        val uri = Uri.fromFile(audioFile)
        val storageRef = FirebaseStorage.getInstance().reference
            .child("chat_audios/${System.currentTimeMillis()}.m4a")

        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    sendMessage(args.groupId, audioUrl = downloadUrl.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Ses kaydı yüklenemedi!", Toast.LENGTH_SHORT).show()
            }
    }


    private fun cancelVoiceRecording() {
        voiceRecorderManager.cancelRecording()
        isRecording = false
        binding.voiceRecorderLayout.visibility = View.GONE
        binding.tvRecordingTimer.stop()
        Toast.makeText(requireContext(), "Kayıt iptal edildi", Toast.LENGTH_SHORT).show()
    }


    private fun loadGroupProfileImage(groupId: String) {
        val groupRef = FirebaseFirestore.getInstance().collection("groups").document(groupId)

        groupRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val imageUrl = document.getString("imageUrl")

                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(imageUrl)
                        .circleCrop()
                        .placeholder(R.drawable.ic_group_placeholder)
                        .into(binding.imgGroupProfile)
                }
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
        val storageRef =
            FirebaseStorage.getInstance().reference.child("chat_images/${System.currentTimeMillis()}.jpg")

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
    }

    private fun sendMessage(
        groupId: String,
        messageText: String? = null,
        imageUrl: String? = null,
        audioUrl :String? = null,
        fileUrl: String? = null

        ) {
        if (messageText.isNullOrEmpty() && imageUrl.isNullOrEmpty() && audioUrl.isNullOrEmpty() && fileUrl.isNullOrEmpty()) {
            Log.e("ChatFragment", "Gönderilecek mesaj yok!")
            return
        }



        auth.currentUser?.uid?.let { userId ->
            viewModel.sendMessage(
                groupId = groupId,
                messageText = messageText,
                imageUrl = imageUrl,
                audioUrl = audioUrl,
                fileUrl = fileUrl, // ✅ eklendi
                senderId = userId
            )

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


                    onlineRef.set(mapOf("status" to true))
                }
            }
    }

}
