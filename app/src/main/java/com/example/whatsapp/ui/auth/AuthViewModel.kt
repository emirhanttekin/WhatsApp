package com.example.whatsapp.ui.auth

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.whatsapp.data.repository.AuthRepository
import com.example.whatsapp.utils.Resource
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _verificationId = MutableLiveData<String?>()
    val verificationId: LiveData<String?> get() = _verificationId

    private val _authState = MutableLiveData<Resource<FirebaseUser?>>()
    val authState: LiveData<Resource<FirebaseUser?>> get() = _authState

    fun sendCode(phoneNumber: String, activity: Activity) {
        authRepository.sendVerificationCode(phoneNumber, activity, object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {}

            override fun onVerificationFailed(e: FirebaseException) {
                _authState.value = Resource.Error(e.message ?: "Doğrulama başarısız")
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                _verificationId.value = verificationId
            }
        })
    }

    fun verifyCode(code: String) {
        val id = verificationId.value
        if (id != null) {
            authRepository.verifyCode(id, code) {
                _authState.value = it
            }
        }
    }
}
