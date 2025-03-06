package com.example.whatsapp.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.whatsapp.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _authState = MutableLiveData<Resource<FirebaseUser?>>()
    val authState: LiveData<Resource<FirebaseUser?>> get() = _authState

    private val _emailVerificationState = MutableLiveData<Resource<Unit>>()
    val emailVerificationState: LiveData<Resource<Unit>> get() = _emailVerificationState

    /**
     * Kullanıcıyı e-posta ve şifre ile kayıt eder.
     */
    fun signUpWithEmail(email: String, password: String) {
        _authState.value = Resource.Loading()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    sendEmailVerification()
                } else {
                    _authState.value = Resource.Error(task.exception?.message ?: "Kayıt başarısız!")
                }
            }
    }

    /**
     * Kullanıcı giriş yapar.
     */
    fun signInWithEmail(email: String, password: String) {
        _authState.value = Resource.Loading()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user?.isEmailVerified == true) {
                        _authState.value = Resource.Success(user)
                    } else {
                        _authState.value = Resource.Error("E-posta doğrulanmadı! Lütfen e-postanızı kontrol edin.")
                    }
                } else {
                    _authState.value = Resource.Error(task.exception?.message ?: "Giriş başarısız!")
                }
            }
    }

    /**
     * Kullanıcıya e-posta doğrulama bağlantısı gönderir.
     */
    private fun sendEmailVerification() {
        val user = auth.currentUser
        user?.sendEmailVerification()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _emailVerificationState.value = Resource.Success(Unit)
                } else {
                    _emailVerificationState.value = Resource.Error("Doğrulama e-postası gönderilemedi!")
                }
            }
    }

    /**
     * Kullanıcının e-posta doğrulama durumunu kontrol eder.
     */
    fun checkEmailVerificationAfterLogin() {
        val user = auth.currentUser
        user?.reload()?.addOnCompleteListener {
            val updatedUser = auth.currentUser
            if (updatedUser != null && updatedUser.isEmailVerified) {
                _authState.value = Resource.Success(updatedUser)
            } else {
                _authState.value = Resource.Error("E-posta doğrulanmadı! Lütfen e-postanızı kontrol edin ve tekrar deneyin.")
            }
        }
    }

    fun checkEmailVerification() {
        val user = auth.currentUser
        user?.reload()?.addOnCompleteListener {
            if (user.isEmailVerified) {
                _authState.value = Resource.Success(user)
            } else {
                _authState.value = Resource.Error("E-posta doğrulanmadı! Lütfen e-postanızı kontrol edin ve tekrar deneyin.")
            }
        } ?: run {
            _authState.value = Resource.Error("Kullanıcı bilgisi bulunamadı!")
        }
    }

    /**
     * Kullanıcı oturumunu kapatır.
     */
    fun signOut() {
        auth.signOut()
        _authState.value = Resource.Success(null)
    }
}
