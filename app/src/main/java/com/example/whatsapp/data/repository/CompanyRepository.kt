package com.example.whatsapp.data.repository

import com.example.whatsapp.data.model.Company
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompanyRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun getCompanies(): List<Company> {
        return try {
            val snapshot = firestore.collection("companies").get().await()
            snapshot.toObjects(Company::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addCompany(company: Company) {
        firestore.collection("companies").add(company).await()
    }
}
