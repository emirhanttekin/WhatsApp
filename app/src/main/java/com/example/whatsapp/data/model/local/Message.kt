package com.example.whatsapp.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")

data class Message(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderId: String,
    val groupId: String,
    val message: String,
    val timestamp: Long
)
