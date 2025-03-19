package com.example.whatsapp.ui.company.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.whatsapp.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CreateCompanyViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _createCompanyState = MutableLiveData<Resource<String>>()
    val createCompanyState: LiveData<Resource<String>> get() = _createCompanyState

    fun createCompany(companyName: String, companyDescription: String) {
        val user = auth.currentUser
        if (user == null) {
            _createCompanyState.value = Resource.Error("Kullanıcı oturum açmamış!")
            return
        }

        val companyId = firestore.collection("companies").document().id
        val companyData = hashMapOf(
            "id" to companyId,
            "name" to companyName,
            "description" to companyDescription,
            "ownerId" to user.uid
        )

        _createCompanyState.value = Resource.Loading()

        firestore.collection("companies").document(companyId)
            .set(companyData)
            .addOnSuccessListener {
                addUserToCompany(user.uid, companyId)
            }
            .addOnFailureListener { e ->
                _createCompanyState.value = Resource.Error("Şirket oluşturulamadı! Hata: ${e.message}")
            }
    }

    private fun addUserToCompany(userId: String, companyId: String) {
        val userCompanyData = hashMapOf(
            "companyId" to companyId,
            "role" to "OWNER"
        )

        firestore.collection("users").document(userId)
            .set(userCompanyData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                _createCompanyState.value = Resource.Success(companyId)
            }
            .addOnFailureListener { e ->
                _createCompanyState.value = Resource.Error("Şirket oluşturuldu ama kullanıcı eklenemedi! Hata: ${e.message}")
            }
    }
}
