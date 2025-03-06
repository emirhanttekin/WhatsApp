package com.example.whatsapp.data.model

data class Group(
    val id: String = "",
    val name: String = "",
    val companyId: String = "",
    val members: List<String> = listOf()
)
