package com.example.whatsapp.data.model

data class Group(
    val id: String = "",
    val name: String = "",
    val companyId: String = "",
    val members: List<String> = emptyList(),
    val ownerId: String = "",
    val imageUrl: String = "",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),

    // 🔹 Kullanıcıya özel okunmamış mesaj sayısını Firestore'dan çekeceğiz
    val unreadMessages: Map<String, Int> = emptyMap()
) {
    var unreadCount: Int = 0 // 🔹 Bu alan liste ekranında kaç unread mesaj varsa gösterecek
}
