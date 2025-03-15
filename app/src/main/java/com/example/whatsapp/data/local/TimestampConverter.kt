package com.example.whatsapp.data.local

import androidx.room.TypeConverter
import com.google.firebase.Timestamp

class TimestampConverter {
    @TypeConverter
    fun fromTimestamp(timestamp: Timestamp?): Long? {
        return timestamp?.seconds // 🔥 Timestamp’i Long’a çeviriyoruz
    }

    @TypeConverter
    fun toTimestamp(seconds: Long?): Timestamp? {
        return seconds?.let { Timestamp(it, 0) } // 🔥 Long’dan Timestamp’e çeviriyoruz
    }
}
