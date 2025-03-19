package com.example.whatsapp.ui.chat.socket

import android.util.Log
import com.google.firebase.Timestamp
import io.socket.client.Socket
import org.json.JSONObject
import io.socket.client.IO
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object SocketManager {
    private const val SERVER_URL = "https://chat-server-node-30af47d1d8c8.herokuapp.com/"

    private var socket: Socket? = null

    fun connectSocket() {
        try {
            socket = IO.socket(SERVER_URL)
            socket?.connect()

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d("Socket", " Socket.IO başarıyla bağlandı!")


                setOnMessageReceivedListener { groupId, senderId, text, senderProfileImageUrl, imageUrl, timestamp ->
                    Log.d(
                        "Socket",
                        " Yeni Mesaj Geldi -> Grup: $groupId, Gönderen: $senderId, Mesaj: $text, Resim: $imageUrl, Zaman: $timestamp"
                    )
                }

            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e("Socket", " Socket bağlantı hatası: ${args[0]}")
            }

        } catch (e: Exception) {
            Log.e("Socket", "Bağlantı hatası: ${e.message}")
        }
    }

    fun disconnectSocket() {
        socket?.disconnect()
        Log.d("Socket", " Socket.IO bağlantısı kesildi")
    }

    fun joinGroup(userId: String, groupId: String) {
        val data = JSONObject()
        data.put("userId", userId)
        data.put("groupId", groupId)
        socket?.emit("joinGroup", data)
        Log.d("Socket", " Kullanıcı gruba katıldı: Kullanıcı ID = $userId, Grup ID = $groupId")
    }

    fun sendMessage(
        groupId: String,
        message: String?,
        senderId: String,
        senderName: String,
        senderProfileImageUrl: String,
        imageUrl: String?
    ) {
        if (message.isNullOrEmpty() && imageUrl.isNullOrEmpty()) {
            Log.e("Socket", " Gönderilecek mesaj veya resim yok, işlem iptal edildi.")
            return
        }

        val data = JSONObject()
        data.put("groupId", groupId)
        data.put("senderId", senderId)
        data.put("senderName", senderName)
        data.put("senderProfileImageUrl", senderProfileImageUrl)

        if (!message.isNullOrEmpty()) {
            data.put("message", message)
        }
        if (!imageUrl.isNullOrEmpty()) {
            data.put("imageUrl", imageUrl)
        }

        socket?.emit("sendMessage", data)

        Log.d(
            "Socket",
            " Yeni mesaj gönderildi: Grup = $groupId, Mesaj = $message, Resim = $imageUrl, Gönderen = $senderId"
        )
    }


    fun setOnMessageReceivedListener(
        onMessageReceived: (String, String, String?, String, String?, Timestamp) -> Unit
    ) {
        Log.d("Socket", "✅ setOnMessageReceivedListener çağrıldı")

        socket?.on("receiveMessage") { args ->
            try {
                Log.d("Socket", "📥 receiveMessage EVENTİ TETİKLENDİ: ${args.contentToString()}")

                if (args.isEmpty() || args[0] == null) {
                    Log.e("Socket", " receiveMessage eventinde veri GELMEDİ!")
                    return@on
                }


                val messageObj = try {
                    args[0] as? JSONObject ?: JSONObject(args[0].toString())
                } catch (e: Exception) {
                    Log.e("Socket", " JSON parse hatası: ${e.message}")
                    return@on
                }


                val groupId = messageObj.optString("groupId", "")
                val text = messageObj.optString("message", null)
                val senderId = messageObj.optString("senderId", "")
                val senderProfileImageUrl = messageObj.optString("senderProfileImageUrl", "")
                val imageUrl = messageObj.optString("imageUrl", null)
                val timestampString = messageObj.optString("timestamp", "")

                if (groupId.isEmpty() || senderId.isEmpty()) {
                    Log.e("Socket", "❌ Eksik veri var!")
                    return@on
                }


                val timestamp = parseTimestamp(timestampString)

                Log.d(
                    "Socket",
                    " Mesaj Alındı: Grup = $groupId, Mesaj = $text, Resim: $imageUrl, Gönderen = $senderId, Zaman = $timestamp"
                )


                onMessageReceived(groupId, senderId, text, senderProfileImageUrl, imageUrl, timestamp)

            } catch (e: Exception) {
                Log.e("Socket", " receiveMessage eventinde hata: ${e.message}")
            }
        }
    }




    private fun parseTimestamp(timestamp: String): Timestamp {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(timestamp)
            Timestamp(date?.time?.div(1000) ?: 0, 0)
        } catch (e: Exception) {
            Log.e("Socket", "❌ Timestamp dönüştürme hatası: ${e.message}")
            Timestamp.now()
        }
    }


}
