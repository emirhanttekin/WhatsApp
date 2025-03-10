package com.example.whatsapp.ui.chat.invite

import android.app.AlertDialog
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class InvitationListener @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) {
    fun startListeningForInvites() {
        val userEmail = auth.currentUser?.email ?: return

        firestore.collection("groupInvitations").document(userEmail)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val groupId = snapshot.getString("groupId") ?: return@addSnapshotListener
                val groupName = snapshot.getString("groupName") ?: "Bilinmeyen Grup"

                AlertDialog.Builder(context)
                    .setTitle("Grup Daveti")
                    .setMessage("$groupName adlı gruba katılmak istiyor musunuz?")
                    .setPositiveButton("Kabul Et") { _, _ ->
                        acceptInvite(userEmail, groupId)
                    }
                    .setNegativeButton("Reddet") { _, _ ->
                        rejectInvite(userEmail)
                    }
                    .show()
            }
    }

    private fun acceptInvite(userEmail: String, groupId: String) {
        firestore.collection("groups").document(groupId)
            .update("members", FieldValue.arrayUnion(userEmail))
            .addOnSuccessListener {
                firestore.collection("groupInvitations").document(userEmail)
                    .delete()
            }
    }

    private fun rejectInvite(userEmail: String) {
        firestore.collection("groupInvitations").document(userEmail)
            .delete()
    }
}
