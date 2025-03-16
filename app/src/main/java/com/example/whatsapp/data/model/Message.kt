package com.example.whatsapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.whatsapp.data.local.TimestampConverter
import com.google.firebase.Timestamp

@Entity(tableName = "messages")
@TypeConverters(TimestampConverter::class) // 🔥 Timestamp için dönüşüm ekliyoruz
data class Message(
    @PrimaryKey val id: String = "",  // ✅ PrimaryKey ekledik
    val senderId: String = "",
    val groupId: String = "",
    val senderName: String = "",
    val senderProfileImageUrl: String = "",
    val message: String = "",
    val timestamp: Timestamp = Timestamp.now() // 🔥 Firestore Timestamp
)
