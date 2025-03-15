package com.example.whatsapp.ui.chat

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.whatsapp.R
import com.example.whatsapp.databinding.FragmentChatBinding
import com.example.whatsapp.ui.chat.adapter.ChatAdapter
import com.example.whatsapp.ui.chat.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ChatFragment : Fragment(R.layout.fragment_chat) {

    private lateinit var binding: FragmentChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private val args: ChatFragmentArgs by navArgs()

    @Inject
    lateinit var chatAdapter: ChatAdapter

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentChatBinding.bind(view)

        val groupId = args.groupId
        val groupName = args.groupName
        binding.tvGroupName.text = groupName

        setupRecyclerView()


        viewModel.loadMessagesFromRoom(groupId)


        viewModel.connectSocket()


        auth.currentUser?.uid?.let { userId ->
            viewModel.joinGroup(userId, groupId)
        }


        viewModel.messagesLiveData.observe(viewLifecycleOwner) { messages ->
            Log.d("ChatFragment", "📨 RecyclerView Güncelleniyor: ${messages.size} mesaj var.")

            chatAdapter.submitList(ArrayList(messages)) {
                binding.rvMessages.postDelayed({
                    if (messages.isNotEmpty()) {
                        binding.rvMessages.smoothScrollToPosition(messages.size - 1)
                    }
                }, 200)
            }
        }

        binding.btnSend.setOnClickListener {
            sendMessage(groupId)
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
}
