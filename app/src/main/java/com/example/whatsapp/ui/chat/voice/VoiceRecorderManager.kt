package com.example.whatsapp.utils.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Environment
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class VoiceRecorderManager(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var audioFilePath: String? = null

    fun startRecording(): String? {
        try {
            val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val outputFile = File(outputDir, "AUD_${timeStamp}.m4a")
            audioFilePath = outputFile.absolutePath

            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(audioFilePath)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                prepare()
                start()
            }

            Log.d("VoiceRecorder", "🎙️ Kayıt başladı: $audioFilePath")
            return audioFilePath

        } catch (e: Exception) {
            Log.e("VoiceRecorder", "❌ Kayıt başlatılamadı: ${e.message}")
            return null
        }
    }

    fun stopRecording(): String? {
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            Log.d("VoiceRecorder", "✅ Kayıt tamamlandı: $audioFilePath")
            audioFilePath
        } catch (e: Exception) {
            Log.e("VoiceRecorder", "❌ Kayıt durdurulamadı: ${e.message}")
            null
        }
    }

    fun cancelRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            audioFilePath?.let { path ->
                File(path).delete()
                Log.d("VoiceRecorder", "🗑️ Kayıt iptal edildi ve silindi: $path")
            }
            audioFilePath = null
        } catch (e: Exception) {
            Log.e("VoiceRecorder", "❌ Kayıt iptal hatası: ${e.message}")
        }
    }
}
