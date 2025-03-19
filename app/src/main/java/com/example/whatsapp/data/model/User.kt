package com.example.whatsapp.data.model

import com.google.firebase.firestore.PropertyName

data class User(
    val uid: String = "",
    val email: String = "",
    val companyId: String? = null,
    val role: String = "member" ,
    val  name : String   = "",
    val  profileImageUrl : String   = ""

)
