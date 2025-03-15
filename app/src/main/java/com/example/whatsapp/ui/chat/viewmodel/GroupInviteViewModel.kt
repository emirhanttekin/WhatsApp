package com.example.whatsapp.ui.chat.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.whatsapp.data.model.GroupInvitation
import com.example.whatsapp.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GroupInviteViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _inviteState = MutableLiveData<Resource<Boolean>>()
    val inviteState: LiveData<Resource<Boolean>> get() = _inviteState

    private val _userRole = MutableLiveData<String>()
    val userRole: LiveData<String> get() = _userRole

    //  Kullanıcının rolünü kontrol et
    fun checkUserRole(groupId: String, userId: String) {
        firestore.collection("groups").document(groupId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val ownerId = document.getString("ownerId") ?: ""
                    Log.d("OWNER_CHECK", "Owner ID: $ownerId - Current User ID: $userId") // ✅ LOG EKLEDİK

                    // Eğer giriş yapan kullanıcı ownerId ile eşleşiyorsa OWNER yap
                    _userRole.value = if (ownerId == userId) "OWNER" else "MEMBER"
                } else {
                    _userRole.value = "MEMBER"
                }
            }
            .addOnFailureListener {
                Log.e("OWNER_CHECK", "Hata: ${it.message}")
            }
    }

    // Kullanıcıya grup daveti gönder
    fun inviteUserToGroup(groupId: String, groupName: String, inviteEmail: String) {
        _inviteState.value = Resource.Loading()

        val inviteData = hashMapOf(
            "groupId" to groupId,
            "groupName" to groupName,
            "receiverEmail" to inviteEmail,
            "status" to "pending",  // Kullanıcının kabul etmesini bekliyoruz
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("groupInvitations").document(inviteEmail)
            .set(inviteData)
            .addOnSuccessListener {
                _inviteState.value = Resource.Success(true)
            }
            .addOnFailureListener { exception ->
                _inviteState.value = Resource.Error("Davet gönderilemedi: ${exception.message}")
            }
    }
}
