package com.example.whatsapp.utils.helper

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.example.whatsapp.receiver.ReminderReceiver
import java.util.Calendar

object ReminderUtils {

    fun showDateTimePicker(context: Context, taskId: String, message: String) {
        val calendar = Calendar.getInstance()

        // 📅 Tarih seçimi
        DatePickerDialog(context, { _, year, month, day ->
            // 🕒 Saat seçimi
            TimePickerDialog(context, { _, hour, minute ->
                calendar.set(year, month, day, hour, minute, 0)
                setAlarm(context, calendar, taskId, message)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun setAlarm(context: Context, calendar: Calendar, taskId: String, message: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 🔒 Android 12+ için alarm izni kontrolü
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(context, "Lütfen hatırlatıcı izni verin!", Toast.LENGTH_LONG).show()

                // Kullanıcıyı ayarlara yönlendir
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return
            }
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("task_message", message)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 🔔 Alarmı ayarla
        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )

        Toast.makeText(context, "⏰ Hatırlatıcı ayarlandı!", Toast.LENGTH_SHORT).show()
    }
    fun showNotification(context: Context, message: String) {
        val channelId = "reminder_channel"
        val channelName = "Görev Hatırlatıcıları"

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                channelName,
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Görev hatırlatma bildirimleri"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("⏰ Görev Süresi Doldu!")
            .setContentText(message)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

}
