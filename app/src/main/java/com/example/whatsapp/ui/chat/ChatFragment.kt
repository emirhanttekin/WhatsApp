package com.example.whatsapp.ui.chat

import android.os.Bundle
import android.util.Log
import android.view.View
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
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ChatFragment : Fragment(R.layout.fragment_chat) {

    private lateinit var binding: FragmentChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private val args: ChatFragmentArgs by navArgs()
    private var isChatScreenVisible: Boolean = false
    @Inject
    lateinit var chatAdapter: ChatAdapter

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentChatBinding.bind(view)

        val groupId = args.groupId
        val groupName = args.groupName

        Log.d("ChatFragment", "✅ ChatFragment → Group ID: $groupId")

        binding.tvGroupName.text = groupName
        setupRecyclerView()
        checkIfUserIsOwner(groupId)

        binding.btnInvite.setOnClickListener {
            val action = ChatFragmentDirections.actionChatFragmentToInviteUserFragment(groupId, groupName)
            findNavController().navigate(action)
        }

        binding.tvGroupName.setOnClickListener {
            val action = ChatFragmentDirections.actionChatFragmentToGroupDetailsFragment(
                groupId = groupId,  // 🤔 Bilerek ters çevirdik test için
                groupName = groupName
            )
            findNavController().navigate(action)

        }




        // 🔥 Socket bağlantısını kur
        viewModel.connectSocket()

        // 🔥 Kullanıcıyı gruba dahil et
        auth.currentUser?.uid?.let { userId ->
            viewModel.joinGroup(userId, groupId)
        }



        // 🔥 LiveData Observer (Mesajları Güncelle)
        viewModel.messagesLiveData.observe(viewLifecycleOwner) { messages ->
            Log.d("ChatFragment", "📨 RecyclerView Güncelleniyor: ${messages.size} mesaj var.")

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
            sendMessage(groupId)
        }
    }



    private fun checkIfUserIsOwner(groupId: String) {
        val currentUserId = auth.currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("groups").document(groupId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val ownerId = document.getString("ownerId") ?: ""
                    Log.d("ChatFragment", "👑 Grup Sahibi ID: $ownerId")

                    // Eğer giriş yapan kullanıcı grup sahibi ise butonu göster
                    if (currentUserId == ownerId) {
                        binding.btnInvite.visibility = View.VISIBLE
                    } else {
                        binding.btnInvite.visibility = View.GONE
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatFragment", "❌ Grup sahibi bilgisi alınamadı: ${e.message}")
            }
    }
    private fun setupRecyclerView() {
        binding.rvMessages.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
        }
    }

    private fun sendMessage(groupId: String) {
        val messageText = binding.etMessage.text.toString().trim()
        if (messageText.isNotEmpty()) {
            auth.currentUser?.uid?.let { userId ->
                viewModel.sendMessage(groupId, messageText, userId)
            }
            binding.etMessage.text?.clear()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.disconnectSocket()
    }

    override fun onResume() {
        super.onResume()
        val groupId = args.groupId
        viewModel.connectSocket()
        viewModel.loadMessagesFromRoom(groupId)


        // 🔥 Kullanıcı sohbet ekranında, bildirim göstermeye gerek yok
        viewModel.isChatScreenVisible = true
    }

    override fun onPause() {
        super.onPause()

        // 🔥 Kullanıcı chat ekranından çıktığında bildirimler aktif hale gelsin
        viewModel.isChatScreenVisible = false
    }


}