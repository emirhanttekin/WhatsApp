package com.example.whatsapp.ui.company.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whatsapp.data.model.Company
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompanyViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _companies = MutableLiveData<List<Company>>()
    val companies: LiveData<List<Company>> get() = _companies

    fun fetchCompanies() {
        firestore.collection("companies").get()
            .addOnSuccessListener { documents ->
                val companyList = documents.map { doc ->
                    val company = Company(
                        id = doc.id,
                        name = doc.getString("name") ?: "Bilinmeyen Şirket",
                        ownerId = doc.getString("ownerId") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                    Log.d("CompanyViewModel", "Şirket çekildi: $company")
                    company
                }
                _companies.value = companyList
            }

            .addOnFailureListener {
                _companies.value = emptyList() // Hata olursa boş liste döndür
            }
    }

}
