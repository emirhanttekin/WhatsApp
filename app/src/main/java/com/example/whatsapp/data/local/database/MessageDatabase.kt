package com.example.whatsapp.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.whatsapp.data.local.MessageDao
import com.example.whatsapp.data.local.TimestampConverter
import com.example.whatsapp.data.model.Message
import com.firebase.ui.auth.BuildConfig

@Database(
    entities = [Message::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(TimestampConverter::class)
abstract class MessageDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: MessageDatabase? = null

        fun getDatabase(context: Context): MessageDatabase {
            return INSTANCE ?: synchronized(this) {
                try {

                    if (BuildConfig.DEBUG) {
                        context.deleteDatabase("chat_database")
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MessageDatabase::class.java,
                    "chat_database"
                )
                    .fallbackToDestructiveMigration() // 👈 schema değişirse DB sıfırdan kurulur
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
