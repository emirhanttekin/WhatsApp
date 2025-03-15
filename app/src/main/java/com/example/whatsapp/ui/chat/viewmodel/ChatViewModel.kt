package com.example.whatsapp.ui.chat.viewmodel
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whatsapp.data.local.MessageDao
import com.example.whatsapp.ui.chat.socket.SocketManager
import com.example.whatsapp.data.model.Message
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageDao: MessageDao
) : ViewModel() {

    private val _messagesLiveData = MutableLiveData<List<Message>>()
    val messagesLiveData: LiveData<List<Message>> get() = _messagesLiveData

    private val messagesList = mutableListOf<Message>()

    private val firestore = FirebaseFirestore.getInstance()
    private val messagesCollection = firestore.collection("messages")



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
        Log.d("ChatViewModel", " Yeni mesaj gönderiliyor: Grup = $groupId, Mesaj = $messageText, Gönderen = $senderId")

        val timestamp = Timestamp.now()
        val message = Message(
            id = System.currentTimeMillis().toString(),
            senderId = senderId,
            groupId = groupId,
            message = messageText,
            timestamp = timestamp
        )


        SocketManager.sendMessage(groupId, messageText, senderId)

        saveMessageToFirebase(message)
        saveMessageToLocal(message)

    }

    private fun listenForMessages() {
        Log.d("ChatViewModel", "⏳ Yeni mesaj dinleniyor...")
        SocketManager.setOnMessageReceivedListener { groupId, senderId, text, timestampString ->
            try {
                Log.d("ChatViewModel", " Yeni Mesaj Alındı -> Grup: $groupId, Gönderen: $senderId, Mesaj: $text, Zaman: $timestampString")

                val timestamp = Timestamp.now()
                val message = Message(
                    id = System.currentTimeMillis().toString(),
                    senderId = senderId,
                    groupId = groupId,
                    message = text,
                    timestamp = timestamp
                )

                messagesList.add(message)
                Log.d("ChatViewModel", " messagesList Güncellendi -> Mesaj Sayısı: ${messagesList.size}")


                _messagesLiveData.postValue(ArrayList(messagesList))


                saveMessageToFirebase(message)


                saveMessageToLocal(message)

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("ChatViewModel", "Mesaj dinleme hatası: ${e.message}")
            }
        }
    }

    fun loadMessagesFromRoom(groupId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val localMessages = messageDao.getMessages(groupId)
            messagesList.clear()
            messagesList.addAll(localMessages)
            _messagesLiveData.postValue(ArrayList(messagesList))
            Log.d("ChatViewModel", "Room'dan mesajlar yüklendi -> Mesaj Sayısı: ${messagesList.size}")
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
