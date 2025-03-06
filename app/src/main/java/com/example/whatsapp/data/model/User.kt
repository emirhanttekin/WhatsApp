package com.example.whatsapp.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val companyId: String? = null,  // Kullanıcı bir şirkete bağlı mı?
    val role: String = "member"     // Varsayılan rol: üye
)
