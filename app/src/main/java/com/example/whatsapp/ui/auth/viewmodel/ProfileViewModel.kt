package com.example.whatsapp.ui.auth

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.whatsapp.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _userProfile = MutableLiveData<UserProfile>()
    val userProfile: LiveData<UserProfile> get() = _userProfile

    private val _updateStatus = MutableLiveData<Boolean>()
    val updateStatus: LiveData<Boolean> get() = _updateStatus

    @SuppressLint("NullSafeMutableLiveData")
    fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userProfile = UserProfile(
                        name = document.getString("name") ?: "",
                        surname = document.getString("surname") ?: "",
                        email = document.getString("email") ?: "",
                        companyId = document.getString("companyId") ?: "",
                        profileImageUrl = document.getString("profileImageUrl") ?: ""
                    )
                    _userProfile.value = userProfile
                }
            }
            .addOnFailureListener {
                _userProfile.value = null
            }
    }

    fun updateUserProfile(name: String, surname: String) {
        val userId = auth.currentUser?.uid ?: return

        val updates = mapOf(
            "name" to name,
            "surname" to surname,

        )

        firestore.collection("users").document(userId).update(updates)
            .addOnSuccessListener {
                _updateStatus.value = true
            }
            .addOnFailureListener {
                _updateStatus.value = false
            }
    }

    fun signOut() {
        auth.signOut()
    }
}
