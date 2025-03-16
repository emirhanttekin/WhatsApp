package com.example.whatsapp.ui.group.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.whatsapp.data.model.GroupDetails
import com.example.whatsapp.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GroupDetailsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _groupDetailsLiveData = MutableLiveData<GroupDetails>()
    val groupDetailsLiveData: LiveData<GroupDetails> = _groupDetailsLiveData

    fun fetchGroupDetails(groupId: String) {
        val groupRef = firestore.collection("groups").document(groupId)

        groupRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val members = document.get("members") as? List<String> ?: emptyList()
                    val ownerId = document.getString("ownerId") ?: ""  // 👈 Grup sahibi ID'si alınıyor.
                    Log.d("GroupDetailsViewModel", "✅ Grup detayları alındı: Owner: $ownerId, Members: $members")
                    fetchUsers(ownerId, members) // Kullanıcı bilgilerini getir
                } else {
                    Log.e("GroupDetailsViewModel", "❌ Grup bulunamadı")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("GroupDetailsViewModel", "🔥 Firestore Hatası: ${exception.localizedMessage}")
            }
    }

    private fun fetchUsers(ownerId: String, memberIds: List<String>) {
        val usersList = mutableListOf<User>()

        for (uid in memberIds) {
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val user = document.toObject(User::class.java)?.copy(uid = uid) // User nesnesine ID ekle
                        user?.let {
                            usersList.add(it)
                            _groupDetailsLiveData.postValue(GroupDetails(ownerId, usersList)) // 👈 Yeni model kullanılıyor.
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("GroupDetailsViewModel", "🔥 Kullanıcı getirme hatası: ${exception.localizedMessage}")
                }
        }
    }
}