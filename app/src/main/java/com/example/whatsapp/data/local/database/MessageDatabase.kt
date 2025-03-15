package com.example.whatsapp.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.whatsapp.data.local.MessageDao
import com.example.whatsapp.data.local.TimestampConverter
import com.example.whatsapp.data.model.Message

@Database(entities = [Message::class], version = 1, exportSchema = false)
@TypeConverters(TimestampConverter::class) // 🔥 TypeConverter burada da olmalı!
abstract class MessageDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: MessageDatabase? = null

        fun getDatabase(context: Context): MessageDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MessageDatabase::class.java,
                    "chat_database"
                )
                    .fallbackToDestructiveMigration() // 🔥 Veritabanını sıfırla (önemli!)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
