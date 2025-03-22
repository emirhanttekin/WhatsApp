package com.example.whatsapp.utils

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyLifecycleObserver : DefaultLifecycleObserver {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        markUserOnline()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        markUserOffline()
    }

    private fun markUserOnline() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("groups")
            .whereArrayContains("members", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { doc ->
                    val groupId = doc.id
                    firestore.collection("groups")
                        .document(groupId)
                        .collection("onlineUsers")
                        .document(userId)
                        .set(mapOf("status" to true))
                }
            }
        Log.d("LifecycleObserver", "✅ Uygulama aktif → Kullanıcı online")
    }

    private fun markUserOffline() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("groups")
            .whereArrayContains("members", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { doc ->
                    val groupId = doc.id
                    firestore.collection("groups")
                        .document(groupId)
                        .collection("onlineUsers")
                        .document(userId)
                        .delete()
                }
            }
        Log.d("LifecycleObserver", "🕓 Uygulama kapandı → Kullanıcı offline")
    }
}
