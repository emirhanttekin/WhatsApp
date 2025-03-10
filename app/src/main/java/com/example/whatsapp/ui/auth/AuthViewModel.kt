package com.example.whatsapp.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.whatsapp.utils.Resource
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _otpState = MutableLiveData<Resource<String>>()  // OTP Gönderme Durumu
    val otpState: LiveData<Resource<String>> get() = _otpState

    private val _verifyState = MutableLiveData<Resource<Boolean>>()  // OTP Doğrulama Durumu
    val verifyState: LiveData<Resource<Boolean>> get() = _verifyState

    private val _authState = MutableLiveData<Resource<FirebaseUser?>>()
    val authState: LiveData<Resource<FirebaseUser?>> get() = _authState

    private val _generatedOtp = MutableLiveData<String?>()  // Gönderilen OTP kodu

    /**
     * 🔹 Kullanıcıyı e-posta ve şifre ile kayıt eder ama önce doğrulama kodu yollar.
     */
    fun registerWithEmail(email: String, password: String) {
        _otpState.value = Resource.Loading()

        val otpCode = (100000..999999).random().toString()  // 🔥 Rastgele 6 haneli kod oluştur
        _generatedOtp.value = otpCode  // Kodun kaydını tut

        val emailData = hashMapOf(
            "email" to email,
            "otp" to otpCode,
            "timestamp" to System.currentTimeMillis(),
            "password" to password
        )

        firestore.collection("email_verifications").document(email)
            .set(emailData)
            .addOnSuccessListener {
                _otpState.value = Resource.Success("Doğrulama kodu gönderildi!")
            }
            .addOnFailureListener { exception ->
                _otpState.value = Resource.Error("Kod gönderilemedi: ${exception.message}")
            }
    }


    /**
     * 🔹 Kullanıcının girdiği kodu doğrular ve kayıt işlemini tamamlar.
     */
    fun verifyEmailOtp(email: String, enteredOtp: String) {
        firestore.collection("email_verifications").document(email)
            .get()
            .addOnSuccessListener { document ->
                val savedOtp = document.getString("otp")
                val savedPassword = document.getString("password")
                if (savedOtp == enteredOtp && savedPassword != null) {
                    createUserWithEmail(email, savedPassword)
                } else {
                    _verifyState.value = Resource.Error("Geçersiz kod! Lütfen tekrar deneyin.")
                }
            }
            .addOnFailureListener {
                _verifyState.value = Resource.Error("Doğrulama başarısız. Lütfen tekrar deneyin!")
            }
    }

    /**
     * 🔹 Firebase'de kullanıcı oluşturur.
     */
    private fun createUserWithEmail(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _verifyState.value = Resource.Success(true)
                    _authState.value = Resource.Success(auth.currentUser)
                } else {
                    _verifyState.value =
                        Resource.Error("Kayıt başarısız! ${task.exception?.message}")
                }
            }
    }

    fun signInWithEmail(email: String, password: String) {
        _authState.value = Resource.Loading()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    _authState.value = Resource.Success(user) // ✅ Direkt giriş yapmasını sağladık!
                } else {
                    _authState.value = Resource.Error(task.exception?.message ?: "Giriş başarısız!")
                }
            }
    }


    fun checkEmailVerificationAfterLogin() {
        val user = auth.currentUser
        user?.reload()?.addOnCompleteListener {
            val updatedUser = auth.currentUser
            if (updatedUser != null && updatedUser.isEmailVerified) {
                _authState.value = Resource.Success(updatedUser)
            } else {
                _authState.value =
                    Resource.Error("E-posta doğrulanmadı! Lütfen e-postanızı kontrol edin ve tekrar deneyin.")
            }
        }
    }

}