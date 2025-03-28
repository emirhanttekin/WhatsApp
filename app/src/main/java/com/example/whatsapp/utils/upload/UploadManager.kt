package com.example.whatsapp.utils.upload

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.google.firebase.storage.FirebaseStorage

object UploadManager {

    fun uploadImage(
        context: Context,
        imageUri: Uri,
        folderName: String = "chat_images",
        onSuccess: (String) -> Unit,
        onFailure: () -> Unit
    ) {
        val storageRef = FirebaseStorage.getInstance().reference
            .child("$folderName/${System.currentTimeMillis()}.jpg")

        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    onSuccess(downloadUrl.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Fotoğraf yüklenemedi!", Toast.LENGTH_SHORT).show()
                onFailure()
            }
    }

    fun uploadAudio(
        context: Context,
        fileUri: Uri,
        folderName: String = "chat_audios",
        onSuccess: (String) -> Unit,
        onFailure: () -> Unit
    ) {
        val storageRef = FirebaseStorage.getInstance().reference
            .child("$folderName/${System.currentTimeMillis()}.m4a")

        storageRef.putFile(fileUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    onSuccess(downloadUrl.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Ses kaydı yüklenemedi!", Toast.LENGTH_SHORT).show()
                onFailure()
            }
    }

    fun uploadDocument(
        context: Context,
        fileUri: Uri,
        folderName: String = "chat_files",
        onSuccess: (String) -> Unit,
        onFailure: () -> Unit
    ) {
        val storageRef = FirebaseStorage.getInstance().reference
            .child("$folderName/${System.currentTimeMillis()}")

        storageRef.putFile(fileUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    onSuccess(downloadUrl.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Belge yüklenemedi!", Toast.LENGTH_SHORT).show()
                onFailure()
            }
    }
}
