package com.example.whatsapp.data.remote

import android.util.Log
import com.example.whatsapp.data.model.Message
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirestoreManager {
    private val db = FirebaseFirestore.getInstance()

    suspend fun saveMessageToFirestore(message: Message) {
        try {
            db.collection("groups")
                .document(message.groupId)
                .collection("messages")
                .document(message.id)
                .set(message)
                .await()
            Log.d("FirestoreManager", "🔥 Mesaj Firestore'a kaydedildi: ${message.message}")
        } catch (e: Exception) {
            Log.e("FirestoreManager", "❌ Firestore mesaj kaydetme hatası: ${e.message}")
        }
    }

    suspend fun getMessagesFromFirestore(groupId: String): List<Message> {
        return try {
            val snapshot = db.collection("groups")
                .document(groupId)
                .collection("messages")
                .orderBy("timestamp")
                .get()
                .await()

            snapshot.toObjects(Message::class.java)
        } catch (e: Exception) {
            Log.e("FirestoreManager", "❌ Firestore mesaj çekme hatası: ${e.message}")
            emptyList()
        }
    }
}
