package com.example.whatsapp.data.repository
import android.app.Activity
import com.example.whatsapp.utils.Resource
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks

interface AuthRepository {
    fun sendVerificationCode(phoneNumber: String, activity: Activity, callback: PhoneAuthProvider.OnVerificationStateChangedCallbacks)
    fun verifyCode(verificationId: String, code: String, result: (Resource<FirebaseUser?>) -> Unit)
}
