package com.example.whatsapp.data.model

data class AssignedTask(
    val id: String = "",
    val messageId: String = "",
    val messageText: String = "",
    val assignerId: String = "",
    val assignerName: String? = null,
    val assignerProfileUrl: String? = null,
    val assigneeId: String = "",
    val assigneeName: String? = null,
    val assignees: List<User> = emptyList(),
    val assigneeProfileUrl: String? = null,
    val groupId: String = "",
    val deadline: Long = 0L,
    val audioUrl: String? = null,
    val fileUrl: String? = null,
    val imageUrl: String? = null,
)
