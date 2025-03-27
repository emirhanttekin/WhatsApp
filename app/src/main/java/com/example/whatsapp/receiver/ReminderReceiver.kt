package com.example.whatsapp.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val message = intent.getStringExtra("task_message") ?: "Hatırlatıcı!"

        // ✅ Kullanıcıya kısa bilgi
        Toast.makeText(context, "⏰ $message", Toast.LENGTH_LONG).show()

        // ✅ Bildirim göster
        showNotification(context, message)
    }

    private fun showNotification(context: Context, message: String) {
        val channelId = "reminder_channel"
        val channelName = "Görev Hatırlatıcıları"

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // ✅ Android 8+ için bildirim kanalı oluştur
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Görev hatırlatma bildirimleri"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // ✅ Bildirim inşası
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("⏰ Görev Zamanı!")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
