package com.example.whatsapp.data.model

data class Group(
    val id: String = "",
    val name: String = "",
    val companyId: String = "",
    val members: List<String> = emptyList(),
    val ownerId: String = "",  // ✅ Grubu oluşturan kişi
    val imageUrl: String = "", // ✅ Grup resmi (Firebase Storage'da tutulacak)
    val description: String = "", // ✅ Grup açıklaması
    val createdAt: Long = System.currentTimeMillis() // ✅ Grup oluşturulma tarihi (Timestamp)
)
