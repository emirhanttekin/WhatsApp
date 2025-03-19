package com.example.whatsapp.ui.group.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.whatsapp.data.model.Group
import com.example.whatsapp.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GroupListViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _groupList = MutableLiveData<Resource<List<Group>>>()
    val groupList: LiveData<Resource<List<Group>>> get() = _groupList

    private val _filteredGroups = MutableLiveData<List<Group>>()
    val filteredGroups: LiveData<List<Group>> get() = _filteredGroups

    private var allGroups: List<Group> = emptyList()

    fun loadGroups() {
        _groupList.value = Resource.Loading()

        val userId = auth.currentUser?.uid ?: return

        firestore.collection("groups")
            .whereArrayContains("members", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val groups = snapshot.documents.mapNotNull { it.toObject(Group::class.java) }
                allGroups = groups
                _groupList.value = Resource.Success(groups)
                _filteredGroups.value = groups
            }
            .addOnFailureListener {
                _groupList.value = Resource.Error("Gruplar yüklenemedi!")
            }
    }

    fun filterGroups(query: String) {
        _filteredGroups.value = if (query.isEmpty()) {
            allGroups
        } else {
            allGroups.filter { it.name.contains(query, ignoreCase = true) }
        }
    }

    fun checkForPendingInvites() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("group_invitations")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    for (doc in snapshot.documents) {
                        val groupId = doc.getString("groupId") ?: continue


                        firestore.collection("groups").document(groupId)
                            .update("members", FieldValue.arrayUnion(userId))
                            .addOnSuccessListener {

                                firestore.collection("group_invitations").document(doc.id)
                                    .update("status", "accepted")
                            }
                    }
                }
            }
    }
}
