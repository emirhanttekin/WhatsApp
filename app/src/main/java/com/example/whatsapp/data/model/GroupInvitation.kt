package com.example.whatsapp.data.model

data class GroupInvitation(
    val groupId: String = "",
    val groupName: String = "",
    val receiverEmail: String = "", // Davet edilen kişinin e-posta adresi
    val status: String = "pending", // `pending`, `accepted`, `rejected`
    val timestamp: Long = System.currentTimeMillis()
)
