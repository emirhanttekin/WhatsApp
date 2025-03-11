package com.example.whatsapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Message(
    val id: String = "",
    val senderId: String = "",
    val groupId: String = "",
    val message: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null // 🔥 Firestore sunucu zamanı
)
