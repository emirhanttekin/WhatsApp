package com.example.whatsapp.utils.sender

import android.util.Log
import com.example.whatsapp.ui.chat.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth

object MessageSender {

    fun sendTextMessage(
        viewModel: ChatViewModel,
        groupId: String,
        message: String
    ) {
        if (message.isBlank()) return

        getCurrentUserId()?.let { userId ->
            viewModel.sendMessage(
                groupId = groupId,
                messageText = message,
                imageUrl = null,
                audioUrl = null,
                fileUrl = null,
                senderId = userId
            )

        }
    }

    fun sendImageMessage(
        viewModel: ChatViewModel,
        groupId: String,
        imageUrl: String
    ) {
        getCurrentUserId()?.let { userId ->
            viewModel.sendMessage(
                groupId = groupId,
                messageText = null,
                imageUrl = imageUrl,
                audioUrl = null,
                fileUrl = null,
                senderId = userId
            )

        }
    }

    fun sendAudioMessage(
        viewModel: ChatViewModel,
        groupId: String,
        audioUrl: String
    ) {
        getCurrentUserId()?.let { userId ->
            viewModel.sendMessage(
                groupId = groupId,
                messageText = null,
                imageUrl = null,
                audioUrl = audioUrl,
                fileUrl = null,
                senderId = userId
            )

        }
    }

    fun sendFileMessage(
        viewModel: ChatViewModel,
        groupId: String,
        fileUrl: String,
    ) {
        getCurrentUserId()?.let { userId ->
            viewModel.sendMessage(
                groupId = groupId,
                messageText = null,
                imageUrl = null,
                audioUrl = null,
                fileUrl = fileUrl,
                senderId = userId
            )

        }
    }

    private fun getCurrentUserId(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid.also {
            if (it == null) Log.e("MessageSender", "❌ Kullanıcı oturumu yok!")
        }
    }
}

