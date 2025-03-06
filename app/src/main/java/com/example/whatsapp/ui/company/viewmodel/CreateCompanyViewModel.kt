package com.example.whatsapp.ui.company.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whatsapp.data.model.Company
import com.example.whatsapp.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateCompanyViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _createCompanyState = MutableLiveData<Resource<Unit>>()
    val createCompanyState: LiveData<Resource<Unit>> get() = _createCompanyState

    fun createCompany(name: String, description: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _createCompanyState.value = Resource.Error("Kullanıcı oturumu açık değil!")
            return
        }

        val companyRef = firestore.collection("companies").document()  // Otomatik ID oluştur
        val company = Company(
            id = companyRef.id, // Firestore’un oluşturduğu ID'yi kaydet
            name = name,
            ownerId = userId,  // Şirketi oluşturan kullanıcı ID'si
            createdAt = System.currentTimeMillis()
        )

        _createCompanyState.value = Resource.Loading()

        companyRef.set(company)
            .addOnSuccessListener {
                _createCompanyState.value = Resource.Success(Unit)
            }
            .addOnFailureListener { e ->
                _createCompanyState.value = Resource.Error("Şirket oluşturulamadı: ${e.message}")
            }
    }
}
