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
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    application: Application, // ✅ AndroidViewModel için gerekli
    private val messageDao: MessageDao
) : AndroidViewModel(application) { // ✅ ViewModel yerine AndroidViewModel kullan

    private val _messagesLiveData = MutableLiveData<List<Message>>()
    val messagesLiveData: LiveData<List<Message>> get() = _messagesLiveData

    private val messagesList = mutableListOf<Message>()
    private val firestore = FirebaseFirestore.getInstance()
    private val messagesCollection = firestore.collection("messages")
    private val auth: FirebaseAuth = FirebaseAuth.getInstance() // ✅ Eksik tanımlamayı ekledik

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


    fun sendMessage(groupId: String, messageText: String, senderId: String) {
        val userRef = firestore.collection("users").document(senderId)

        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val senderName = document.getString("name") ?: "Bilinmeyen"
                val senderProfileImageUrl = document.getString("profileImageUrl") ?: ""

                val timestamp = Timestamp.now()
                val messageId = "${groupId}_${timestamp.seconds}" // 🔥 Unik ID ekle

                val message = Message(
                    id = messageId, // 🔥 ID'yi belirledik
                    senderId = senderId,
                    senderName = senderName,
                    senderProfileImageUrl = senderProfileImageUrl,
                    groupId = groupId,
                    message = messageText,
                    timestamp = timestamp
                )

                // 🔥 Mesajı Room’a kaydet
                saveMessageToLocal(message)

                // 🔥 Firestore’a kaydet
                saveMessageToFirebase(message)

                // 🔥 Socket ile gönder
                SocketManager.sendMessage(groupId, messageText, senderId, senderName, senderProfileImageUrl)
            }
        }
    }



    private fun listenForMessages() {
        Log.d("ChatViewModel", "⏳ Yeni mesaj dinleniyor...")

        SocketManager.setOnMessageReceivedListener { groupId, senderId, text, senderProfileImageUrl ->
            try {
                val timestamp = Timestamp.now()
                val messageId = "${groupId}_${timestamp.seconds}" // 🔥 Socket’ten gelen mesajın ID’si

                // 🔥 Mesaj zaten varsa tekrar eklemeyi önle
                if (messagesList.any { it.id == messageId }) {
                    Log.w("ChatViewModel", "⚠ Socket'ten gelen mesaj zaten var, tekrar eklenmeyecek!")
                    return@setOnMessageReceivedListener
                }

                val message = Message(
                    id = messageId, // 🔥 ID kontrolü yaptık
                    senderId = senderId,
                    senderProfileImageUrl = senderProfileImageUrl,
                    groupId = groupId,
                    message = text,
                    timestamp = timestamp
                )

                saveMessageToLocal(message)

                messagesList.add(message)
                _messagesLiveData.postValue(ArrayList(messagesList))

                Log.d("ChatViewModel", "✅ Yeni mesaj eklendi: $text")

            } catch (e: Exception) {
                Log.e("ChatViewModel", "❌ Mesaj dinleme hatası: ${e.message}")
            }
        }
    }




    private fun getSenderAndGroupInfo(senderId: String, groupId: String, message: String) {
        firestore.collection("users").document(senderId).get()
            .addOnSuccessListener { userDoc ->
                if (userDoc.exists()) {
                    val senderName = userDoc.getString("name") ?: "Bilinmeyen"

                    firestore.collection("groups").document(groupId).get()
                        .addOnSuccessListener { groupDoc ->
                            if (groupDoc.exists()) {
                                val groupName = groupDoc.getString("groupName") ?: "Bilinmeyen Grup"

                                // **Eğer uygulama açık değilse ve kullanıcı login durumundaysa, bildirim göster**
                                if (!isAppInForeground() && isUserLoggedIn()) {
                                    NotificationHelper.showNotification(
                                        getApplication<Application>().applicationContext, // ✅ HATA DÜZELTİLDİ
                                        groupName,
                                        senderName,
                                        message
                                    )
                                }
                            }
                        }
                }
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

    fun loadMessagesFromRoom(groupId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val localMessages = messageDao.getMessages(groupId)

            // 🔥 Eğer mesajlar zaten ekliyse, tekrar eklemeyi önle
            if (messagesList.isNotEmpty()) {
                Log.w("ChatViewModel", "⚠ Zaten mesajlar var, tekrar yüklenmeyecek!")
                return@launch
            }

            messagesList.clear()
            messagesList.addAll(localMessages)
            _messagesLiveData.postValue(ArrayList(messagesList))

            Log.d("ChatViewModel", "✅ Room'dan mesajlar yüklendi -> Mesaj Sayısı: ${messagesList.size}")
        }
    }



    private fun saveMessagesToLocal(messages: List<Message>) {
        viewModelScope.launch(Dispatchers.IO) {
            messageDao.insertMessages(messages)  // 🔥 **Tek tek değil, topluca kaydediyoruz**
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
     fun listenForFirestoreMessages(groupId: String) {
        firestore.collection("messages")
            .whereEqualTo("groupId", groupId)
            .orderBy("timestamp") // 🔥 Zaman sırasına göre sırala
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("ChatViewModel", "Firestore mesaj dinleme hatası: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    for (doc in snapshots.documentChanges) {
                        val message = doc.document.toObject(Message::class.java)

                        // 🔥 Eğer mesaj Room veritabanında yoksa ekle
                        viewModelScope.launch(Dispatchers.IO) {
                            val existingMessage = messageDao.getMessageById(message.id)
                            if (existingMessage == null) {
                                messageDao.insertMessage(message)
                                messagesList.add(message)
                                _messagesLiveData.postValue(ArrayList(messagesList))
                            }
                        }

                        // 🔥 Kullanıcı sohbet ekranında değilse bildirim gönder
                        if (!isAppInForeground()) {
                            getSenderAndGroupInfo(message.senderId, groupId, message.message)
                        }
                    }
                }
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
}
