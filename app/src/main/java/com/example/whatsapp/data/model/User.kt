package com.example.whatsapp.data.model

import com.google.firebase.firestore.PropertyName

data class User(
    val uid: String = "",
    val email: String = "",
    val companyId: String? = null,  // Kullanıcı bir şirkete bağlı mı?
    val role: String = "member" ,// Varsayılan rol: üye
    val  name : String   = "",
    val  profileImageUrl : String   = ""

)
