package com.example.whatsapp.ui.chat.viewmodel

import android.app.Application
import android.app.ActivityManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.whatsapp.data.local.MessageDao
import com.example.whatsapp.ui.chat.socket.SocketManager
import com.example.whatsapp.data.model.Message
import com.example.whatsapp.utils.helper.NotificationHelper
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    application: Application,
    private val messageDao: MessageDao
) : AndroidViewModel(application) {

    private val _messagesLiveData = MutableLiveData<List<Message>>()
    val messagesLiveData: LiveData<List<Message>> get() = _messagesLiveData
    var isChatScreenVisible = false
    private val messagesList = mutableListOf<Message>()
    private val firestore = FirebaseFirestore.getInstance()
    private val messagesCollection = firestore.collection("messages")
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun connectSocket() {
        Log.d("ChatViewModel", "⏳ Socket bağlantısı sağlanıyor...")
        SocketManager.connectSocket()
        listenForMessages()
    }

    fun disconnectSocket() {
        Log.d("ChatViewModel", " Socket bağlantısı kesiliyor...")
        SocketManager.disconnectSocket()
    }

    fun joinGroup(userId: String, groupId: String) {
        Log.d("ChatViewModel", " Kullanıcı gruba katılıyor: Kullanıcı ID = $userId, Grup ID = $groupId")
        SocketManager.joinGroup(userId, groupId)
    }

    fun sendMessage(
        groupId: String,
        messageText: String?,
        imageUrl: String?,
        audioUrl: String?,
        fileUrl: String?, // ✅ eklendi
        senderId: String
    )
    {
        val userRef = firestore.collection("users").document(senderId)

        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val senderName = document.getString("name") ?: "Bilinmeyen"
                val senderProfileImageUrl = document.getString("profileImageUrl") ?: ""
                val timestamp = Timestamp.now()
                val messageId = "${groupId}_${timestamp.seconds}"


                val messageContent = when {
                    !messageText.isNullOrEmpty() -> messageText
                    !audioUrl.isNullOrBlank() && audioUrl != "null" -> "[Sesli mesaj]"
                    !imageUrl.isNullOrBlank() && imageUrl != "null" -> "[Görsel mesaj]"
                    !fileUrl.isNullOrBlank() && fileUrl != "null" -> "[Dosya]"
                    else -> ""
                }

                val message = Message(
                    id = messageId,
                    senderId = senderId,
                    senderName = senderName,
                    senderProfileImageUrl = senderProfileImageUrl,
                    groupId = groupId,
                    message = messageContent,
                    imageUrl = imageUrl,
                    audioUrl = audioUrl,
                    fileUrl = fileUrl,
                    timestamp = timestamp
                )


                Log.d("ChatViewModel", " Mesaj Gönderiliyor: ID = $messageId, İçerik = ${message.message}")

                saveMessageToLocal(message)

                messagesCollection.document(message.id).set(message)
                    .addOnSuccessListener {
                        Log.d("ChatViewModel", "Firestore'a Mesaj Kaydedildi: $messageContent")
                    }
                    .addOnFailureListener { e ->
                        Log.e("ChatViewModel", " Firestore mesaj hatası: ${e.message}")
                    }

                firestore.collection("groups").document(groupId)
                    .get()
                    .addOnSuccessListener { groupDoc ->
                        val members = groupDoc.get("members") as? List<String> ?: emptyList()
                        val updates = mutableMapOf<String, Any>()

                        for (memberId in members) {
                            if (memberId != senderId) {
                                updates["unreadMessages.$memberId"] = FieldValue.increment(1)
                            }
                        }

                        firestore.collection("groups").document(groupId).update(updates)
                            .addOnSuccessListener {
                                Log.d("ChatViewModel", " Unread count güncellendi: $updates")
                            }
                            .addOnFailureListener { e ->
                                Log.e("ChatViewModel", " Unread count güncellenemedi: ${e.message}")
                            }
                    }


                SocketManager.sendMessage(
                    groupId = groupId,
                    message = messageText,
                    senderId = senderId,
                    senderName = senderName,
                    senderProfileImageUrl = senderProfileImageUrl,
                    imageUrl = imageUrl,
                    audioUrl = audioUrl,
                    fileUrl = fileUrl // ✅ burası eklendi
                )


                if (messagesList.any { it.id == messageId }) {
                    Log.w("ChatViewModel", "⚠ Mesaj zaten var, tekrar eklenmeyecek!")
                    return@addOnSuccessListener
                }

            }
        }
    }




    fun listenForMessages() {
        Log.d("ChatViewModel", " Yeni mesajlar dinleniyor...")

        SocketManager.setOnMessageReceivedListener { groupId, senderId, text, senderProfileImageUrl, imageUrl, audioUrl, fileUrl, timestamp, senderName ->

        val messageId = "${groupId}_${timestamp.seconds}"

            if (messagesList.any { it.id == messageId }) {
                Log.w("ChatViewModel", "⚠ Mesaj zaten var, tekrar eklenmeyecek!")
                return@setOnMessageReceivedListener
            }

            val messageContent = when {
                !text.isNullOrEmpty() -> text
                !audioUrl.isNullOrEmpty() && audioUrl != "null" -> "[Sesli mesaj]"
                !imageUrl.isNullOrEmpty() && imageUrl != "null" -> "[Görsel mesaj]"
                !fileUrl.isNullOrEmpty() && fileUrl != "null" -> "[Dosya]" // ✅ bunu ekle
                else -> return@setOnMessageReceivedListener
            }



            val message = Message(
                id = messageId,
                senderId = senderId,
                senderName = senderName,
                senderProfileImageUrl = senderProfileImageUrl,
                groupId = groupId,
                message = messageContent,
                imageUrl = imageUrl,
                audioUrl = audioUrl,
                fileUrl = fileUrl, // ✅ burası yeni
                timestamp = timestamp
            )


            saveMessageToLocal(message)
            messagesList.add(message)
            _messagesLiveData.postValue(ArrayList(messagesList))

            if (!isChatScreenVisible && isUserLoggedIn()) {
                sendNotification(message)
            }

            Log.d("ChatViewModel", " Yeni mesaj eklendi: $messageContent")
        }
    }



    fun loadMessagesFromFirestore(groupId: String) {
        Log.d("ChatViewModel", " Firestore'dan mesajları çekiyoruz...")

        firestore.collection("messages")
            .whereEqualTo("groupId", groupId)
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { documents ->
                val messages = mutableListOf<Message>()
                for (document in documents) {
                    val message = document.toObject(Message::class.java)

                    if (messagesList.any { it.id == message.id }) {
                        continue
                    }

                    messages.add(message)
                }

                if (messages.isNotEmpty()) {
                    messagesList.addAll(messages)
                    _messagesLiveData.postValue(ArrayList(messagesList))
                    saveMessagesToLocal(messages)
                }

                Log.d("ChatViewModel", "Firestore'dan ${messages.size} yeni mesaj yüklendi.")
            }
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", " Firestore mesajlarını yüklerken hata: ${e.message}")
            }
    }

    fun loadMessagesFromRoom(groupId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val localMessages = messageDao.getMessages(groupId)

            Log.d("ChatViewModel", " Room’dan çekilen mesajlar: ${localMessages.size} adet")
            localMessages.forEach { Log.d("ChatViewModel", " Room Mesajı: ${it.message}") }

            messagesList.addAll(localMessages)
            _messagesLiveData.postValue(ArrayList(messagesList))

            Log.d("ChatViewModel", " Room’dan mesajlar UI’a yansıtıldı.")
        }
    }

    fun clearActiveGroup() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .update("activeGroupId", null)
    }

    fun markActiveGroup(groupId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .update("activeGroupId", groupId)
    }

    private fun saveMessagesToLocal(messages: List<Message>) {
        viewModelScope.launch(Dispatchers.IO) {
            messageDao.insertMessages(messages)
            Log.d("ChatViewModel", "Room Database'e mesajlar kaydedildi -> ${messages.size} mesaj")
        }
    }

    private fun saveMessageToFirebase(message: Message) {
        messagesCollection.document(message.id).get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    messagesCollection.document(message.id).set(message)
                        .addOnSuccessListener {
                            Log.d("ChatViewModel", "Firebase'e mesaj eklendi: ${message.message}")
                            cleanupOldMessagesFromFirebase()
                        }
                        .addOnFailureListener { e ->
                            Log.e("ChatViewModel", " Firebase'e mesaj ekleme hatası: ${e.message}")
                        }
                }
            }
    }

    private fun sendNotification(message: Message) {
        val context = getApplication<Application>().applicationContext
        val userId = auth.currentUser?.uid ?: return


        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { userDoc ->
                val activeGroupId = userDoc.getString("activeGroupId")


                if (activeGroupId == message.groupId) {
                    Log.d("ChatViewModel", "📵 Bildirim gönderilmedi. Kullanıcı şu an bu grupta: $activeGroupId")
                    return@addOnSuccessListener
                }


                firestore.collection("groups").document(message.groupId)
                    .get()
                    .addOnSuccessListener { groupDoc ->
                        val groupName = groupDoc.getString("groupName") ?: "Bilinmeyen Grup"

                        NotificationHelper.showNotification(
                            context,
                            groupName = groupName,
                            senderName = message.senderName,
                            message = message.message ?: ""
                        )

                        Log.d("ChatViewModel", "🔔 Bildirim gönderildi: ${message.message}")
                    }
                    .addOnFailureListener { e ->
                        Log.e("ChatViewModel", "❌ Bildirim için grup adı alınamadı: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "❌ Kullanıcının aktif grubu alınamadı: ${e.message}")
            }
    }



    private fun saveMessageToLocal(message: Message) {
        viewModelScope.launch(Dispatchers.IO) {
            messageDao.insertMessage(message)
            Log.d("ChatViewModel", " Room Database'e mesaj eklendi: ${message.message}")
        }
    }

    private fun cleanupOldMessagesFromFirebase() {
        val cutoffTime = Timestamp.now().seconds - (30 * 24 * 60 * 60)

        messagesCollection.whereLessThan("timestamp", Timestamp(cutoffTime, 0))
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { doc ->
                    doc.reference.delete()
                        .addOnSuccessListener {
                            Log.d("ChatViewModel", " 30 günden eski mesaj silindi -> ID: ${doc.id}")
                        }
                        .addOnFailureListener { e ->
                            Log.e("ChatViewModel", " Eski mesajı silme hatası: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", " Eski mesajları temizleme hatası: ${e.message}")
            }
    }

    private fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    private fun isAppInForeground(): Boolean {
        val activityManager = getApplication<Application>().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses
        return appProcesses?.any { it.processName == getApplication<Application>().packageName && it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND } ?: false
    }
}