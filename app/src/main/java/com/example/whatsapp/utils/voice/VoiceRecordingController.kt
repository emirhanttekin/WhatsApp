package com.example.whatsapp.utils.voice

import android.content.Context
import android.os.SystemClock
import android.view.View
import android.widget.Chronometer
import android.widget.Toast
import com.example.whatsapp.utils.helper.PermissionHelper

class VoiceRecordingController(
    private val context: Context,
    private val voiceRecorderManager: VoiceRecorderManager,
    private val recorderLayout: View,
    private val timerView: Chronometer,
    private val onRecordingFinished: (String) -> Unit,
    private val onRecordingCancelled: (() -> Unit)? = null
) {

    private var isRecording = false
    private var recordingStartX = 0f

    fun setupMicTouchListener(micButton: View): View.OnTouchListener {
        return View.OnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    recordingStartX = event.x
                    startRecording()
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    val diffX = event.x - recordingStartX
                    if (diffX < -200) {
                        cancelRecording()
                    }
                    true
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    if (isRecording) startRecording()
                    true
                }
                else -> false
            }
        }
    }
    fun startRecordingManually() {
        val audioPath = voiceRecorderManager.startRecording()
        if (audioPath != null) {
            recorderLayout.visibility = View.VISIBLE
            timerView.base = SystemClock.elapsedRealtime()
            timerView.start()
        }
    }

    fun stopRecordingManually() {
        val audioPath = voiceRecorderManager.stopRecording()
        recorderLayout.visibility = View.GONE
        timerView.stop()
        audioPath?.let { onRecordingFinished(it) }
    }

    private fun startRecording() {
        if (!PermissionHelper.isPermissionGranted(context, android.Manifest.permission.RECORD_AUDIO)) {
            Toast.makeText(context, "Mikrofon izni gerekli!", Toast.LENGTH_SHORT).show()
            return
        }

        val audioPath = voiceRecorderManager.startRecording()
        if (audioPath != null) {
            isRecording = true
            recorderLayout.visibility = View.VISIBLE
            timerView.base = SystemClock.elapsedRealtime()
            timerView.start()
        } else {
            Toast.makeText(context, "Kayıt başlatılamadı!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        val audioPath = voiceRecorderManager.stopRecording()
        isRecording = false
        recorderLayout.visibility = View.GONE
        timerView.stop()

        audioPath?.let {
            onRecordingFinished(it)
        }
    }

    private fun cancelRecording() {
        voiceRecorderManager.cancelRecording()
        isRecording = false
        recorderLayout.visibility = View.GONE
        timerView.stop()
        Toast.makeText(context, "Kayıt iptal edildi", Toast.LENGTH_SHORT).show()
        onRecordingCancelled?.invoke()
    }
}
