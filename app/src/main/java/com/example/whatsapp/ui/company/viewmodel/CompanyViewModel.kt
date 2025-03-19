package com.example.whatsapp.ui.company.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.whatsapp.data.model.Company
import com.example.whatsapp.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CompanyViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _companyState = MutableLiveData<Resource<Company?>>()
    val companyState: LiveData<Resource<Company?>> get() = _companyState

    fun getUserCompany() {
        _companyState.value = Resource.Loading()

        val userId = auth.currentUser?.uid ?: return
        firestore.collection("companies")
            .whereEqualTo("ownerId", userId)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val company = result.documents.first().toObject(Company::class.java)
                    _companyState.value = Resource.Success(company)
                } else {
                    _companyState.value = Resource.Success(null)
                }
            }
            .addOnFailureListener { exception ->
                _companyState.value = Resource.Error(exception.message ?: "Şirket bulunamadı!")
            }
    }
}
