package com.example.whatsapp.ui.chat

import android.os.Bundle
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
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ChatFragment : Fragment(R.layout.fragment_chat) {

    private lateinit var binding: FragmentChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private val args: ChatFragmentArgs by navArgs()

    @Inject
    lateinit var chatAdapter: ChatAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentChatBinding.bind(view)

        val groupId = args.groupId
        val groupName = args.groupName
        binding.tvGroupName.text = groupName

        setupRecyclerView()

        // 📌 Mesajları dinlemeye başla
        viewModel.listenForMessages(groupId)

        // 📌 LiveData değiştikçe mesajları UI'ya yansıt
        viewModel.messagesLiveData.observe(viewLifecycleOwner) { messages ->
            chatAdapter.submitList(messages) {
                // 🔥 Adapter güncellendikten sonra en son mesaja kaydır!
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

        binding.btnInvite.setOnClickListener {
            val action = ChatFragmentDirections
                .actionChatFragmentToInviteUserFragment(groupId, groupName)
            findNavController().navigate(action)
        }
    }

    private fun setupRecyclerView() {
        binding.rvMessages.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true  // 📌 En son mesaj hep en altta gözüksün
            }
        }
    }

    private fun sendMessage(groupId: String) {
        val messageText = binding.etMessage.text.toString().trim()
        if (messageText.isNotEmpty()) {
            viewModel.sendMessage(groupId, messageText)
            binding.etMessage.text?.clear()

            // 🔥 En son mesaja kaydırmayı güvenli hale getir
            binding.rvMessages.postDelayed({
                if (chatAdapter.itemCount > 0) {
                    binding.rvMessages.smoothScrollToPosition(chatAdapter.itemCount - 1)
                }
            }, 300) // 300ms gecikme ile UI güncellendikten sonra kaydır
        }
    }

}
