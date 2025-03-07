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
class CreateGroupViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _createGroupState = MutableLiveData<Resource<Unit>>()
    val createGroupState: LiveData<Resource<Unit>> get() = _createGroupState

    fun createGroup(groupName: String, companyId: String) {
        val user = auth.currentUser
        if (user == null) {
            _createGroupState.value = Resource.Error("Kullanıcı oturum açmamış!")
            return
        }

        val groupId = firestore.collection("groups").document().id
        val groupData = hashMapOf(
            "id" to groupId,
            "name" to groupName,
            "companyId" to companyId,
            "members" to listOf(user.uid)  // Grubu oluşturan kişi otomatik üye olur
        )

        _createGroupState.value = Resource.Loading()

        firestore.collection("groups").document(groupId)
            .set(groupData)
            .addOnSuccessListener {
                _createGroupState.value = Resource.Success(Unit)
            }
            .addOnFailureListener { e ->
                _createGroupState.value = Resource.Error(e.message ?: "Grup oluşturulamadı!")
            }
    }
}
