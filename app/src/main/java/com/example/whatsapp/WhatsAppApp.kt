package com.example.whatsapp
import android.app.Application
import com.google.firebase.FirebaseApp
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.google.GoogleEmojiProvider
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WhatsAppApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        EmojiManager.install(GoogleEmojiProvider())
    }
}