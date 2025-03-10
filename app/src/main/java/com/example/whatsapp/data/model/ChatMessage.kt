package com.example.whatsapp.data.model

data class ChatMessage(
    val senderId: String = "",
    val message: String = "",
    val timestamp: Long = 0
)
