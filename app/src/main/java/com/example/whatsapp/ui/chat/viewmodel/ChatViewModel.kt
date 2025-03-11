package com.example.whatsapp.ui.chat.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.whatsapp.data.model.Message
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _messagesLiveData = MutableLiveData<List<Message>>()
    val messagesLiveData: LiveData<List<Message>> get() = _messagesLiveData

    fun sendMessage(groupId: String, messageText: String) {
        val userId = auth.currentUser?.uid ?: return

        val messageRef = firestore.collection("messages").document()
        val message = Message(
            id = messageRef.id,
            senderId = userId,
            groupId = groupId,
            message = messageText,
            timestamp = null // 🔥 Sunucu zamanı eklenecek
        )

        messageRef.set(message)
            .addOnSuccessListener {
                println("✅ Mesaj başarıyla Firestore'a kaydedildi!")
            }
            .addOnFailureListener { e ->
                println("❌ Mesaj gönderilirken hata oluştu: ${e.message}")
            }
    }



    fun listenForMessages(groupId: String) {
        firestore.collection("messages")
            .whereEqualTo("groupId", groupId)
            .orderBy("timestamp", Query.Direction.ASCENDING) // 🔥 En eski mesaja göre sıralama
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("🔥 Hata: Mesajları çekerken hata oluştu: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { it.toObject(Message::class.java) }
                    _messagesLiveData.postValue(messages)
                }
            }
    }


}
