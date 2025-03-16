package com.example.whatsapp.ui.chat.socket
import android.util.Log
import io.socket.client.Socket
import org.json.JSONObject
import io.socket.client.IO

object  SocketManager {
    private const val SERVER_URL = "https://chat-server-node-30af47d1d8c8.herokuapp.com/"


    private var socket : Socket? = null

    fun connectSocket() {
        try {
            socket = IO.socket(SERVER_URL)
            socket?.connect()

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d("Socket", "✅ Socket.IO başarıyla bağlandı!")

                // 📌 setOnMessageReceivedListener DOĞRU ŞEKİLDE ÇAĞRILIYOR
                setOnMessageReceivedListener { groupId, senderId, text,  senderProfileImageUrl ->
                    Log.d("Socket", "📩 Yeni Mesaj Geldi -> Grup: $groupId, Gönderen: $senderId, Mesaj: $text, Time: , Resim: $senderProfileImageUrl")
                }
            }


            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e("Socket", "❌ Socket bağlantı hatası: ${args[0]}")
            }

        } catch (e: Exception) {
            Log.e("Socket", "Bağlantı hatası: ${e.message}")
        }
    }



    fun disconnectSocket() {
        socket?.disconnect()
        Log.d("Socket", " Socket.IO bağlantısı kesildi")
    }

    fun joinGroup(userId : String, groupId : String) {
        val data = JSONObject()
        data.put("userId", userId)
        data.put("groupId", groupId)
        socket?.emit("joinGroup", data)
        Log.d("Socket", "✅ Kullanıcı gruba katıldı: Kullanıcı ID = $userId, Grup ID = $groupId")
    }

    fun sendMessage(groupId: String, message: String, senderId: String, senderName: String, senderProfileImageUrl: String) {
        val data = JSONObject()
        data.put("groupId", groupId)
        data.put("message", message)
        data.put("senderId", senderId)
        data.put("senderName", senderName)  // ✅ Kullanıcı adı gönderiliyor
        data.put("senderProfileImageUrl", senderProfileImageUrl)  // ✅ PROFİL FOTOĞRAFI EKLENDİ

        socket?.emit("sendMessage", data)

        Log.d("Socket", "✅ Yeni mesaj gönderildi: Grup = $groupId, Mesaj = $message, Gönderen = $senderId, Resim = $senderProfileImageUrl")
    }



    fun setOnMessageReceivedListener(onMessageReceived: (String, String, String, String) -> Unit) {
        Log.d("Socket", "✅ setOnMessageReceivedListener çağrıldı")

        socket?.on("receiveMessage") { args ->
            try {
                Log.d("Socket", "📥 receiveMessage EVENTİ TETİKLENDİ: ${args.contentToString()}")

                if (args.isEmpty() || args[0] == null) {
                    Log.e("Socket", "❌ receiveMessage eventinde veri GELMEDİ!")
                    return@on
                }

                // ✅ **Gelen veriyi JSON olarak işle**
                val messageObj = try {
                    args[0] as? JSONObject ?: JSONObject(args[0].toString()) // **JSON parse et**
                } catch (e: Exception) {
                    Log.e("Socket", "❌ JSON parse hatası: ${e.message}")
                    return@on
                }

                // 📌 **Gelen JSON'dan verileri al**
                val groupId = messageObj.optString("groupId", "")
                val text = messageObj.optString("message", "")
                val senderId = messageObj.optString("senderId", "")
                val senderProfileImageUrl = messageObj.optString("senderProfileImageUrl", "")
                val timestamp = messageObj.optString("timestamp", "")

                if (groupId.isEmpty() || text.isEmpty() || senderId.isEmpty()) {
                    Log.e("Socket", "❌ Eksik veri var!")
                    return@on
                }

                Log.d("Socket", "📥 Mesaj Alındı: Grup = $groupId, Mesaj = $text, Gönderen = $senderId, Resim: $senderProfileImageUrl")

                // **Listener'ı tetikle**
                onMessageReceived(groupId, senderId, text, senderProfileImageUrl)

            } catch (e: Exception) {
                Log.e("Socket", "❌ receiveMessage eventinde hata: ${e.message}")
            }
        }
    }










}