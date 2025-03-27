package com.example.whatsapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.whatsapp.data.local.TimestampConverter
import com.google.firebase.Timestamp

@Entity(tableName = "messages")
@TypeConverters(TimestampConverter::class)
data class Message(
    @PrimaryKey val id: String = "",
    val senderId: String = "",
    val groupId: String = "",
    val senderName: String = "",
    val senderProfileImageUrl: String = "",
    val message: String? = "",
    val imageUrl: String? = null,
    val timestamp: Timestamp = Timestamp.now(),
    val audioUrl: String? = null,
    val fileUrl: String? = null,
    var assigneeName: String? = null,
    var assigneeProfileUrl: String? = null,



    )
