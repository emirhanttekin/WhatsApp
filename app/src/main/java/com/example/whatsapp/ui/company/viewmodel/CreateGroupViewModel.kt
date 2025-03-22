package com.example.whatsapp.ui.company.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.whatsapp.data.model.Group
import com.example.whatsapp.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.util.UUID

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _createGroupState = MutableLiveData<Resource<Group>>()
    val createGroupState: LiveData<Resource<Group>> get() = _createGroupState

    fun createGroup(companyId: String, groupName: String, groupImageUrl: String) {
        _createGroupState.value = Resource.Loading()

        val groupId = UUID.randomUUID().toString()
        val userId = auth.currentUser?.uid ?: return

        val group = Group(
            id = groupId,
            name = groupName,
            companyId = companyId,
            imageUrl = groupImageUrl,
            members = listOf(userId),
            ownerId = userId
        )

        firestore.collection("groups").document(groupId)
            .set(group)
            .addOnSuccessListener {
                _createGroupState.value = Resource.Success(group)
            }
            .addOnFailureListener { e ->
                _createGroupState.value = Resource.Error(e.message ?: "Grup oluşturulamadı!")
            }
    }

}
