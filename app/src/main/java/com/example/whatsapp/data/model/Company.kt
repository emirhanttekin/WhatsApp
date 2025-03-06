package com.example.whatsapp.data.model

data class Company(
    val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
