package com.example.whatsapp.ui.assigntask.adapter

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.whatsapp.R
import com.example.whatsapp.data.model.AssignedTask
import com.example.whatsapp.databinding.ItemAssignedTaskBinding
import com.example.whatsapp.utils.helper.ReminderUtils
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AssignedTaskAdapter @Inject constructor() :
    ListAdapter<AssignedTask, AssignedTaskAdapter.TaskViewHolder>(DiffCallback()) {

    inner class TaskViewHolder(val binding: ItemAssignedTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private var mediaPlayer: MediaPlayer? = null
        private var isPlaying = false
        private var audioTimer: CountDownTimer? = null
        private var countDownTimer: CountDownTimer? = null

        fun bind(task: AssignedTask) {
            val formattedDeadline = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
                .format(Date(task.deadline))

            binding.tvMessage.text = task.messageText
            binding.tvDeadline.text = formattedDeadline

            // 📌 Görev Atayan Bilgisi
            binding.tvAssignerName.text = task.assignerName ?: "Görevi Atayan"
            if (!task.assignerProfileUrl.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(task.assignerProfileUrl)
                    .circleCrop()
                    .into(binding.imgAssignerProfile)
            } else {
                binding.imgAssignerProfile.setImageResource(R.drawable.ic_profile_placeholder)
            }

            // ✅ Atanan Kişiler (Toplu Gösterim)
            binding.assignedUsersContainer.removeAllViews()
            val context = binding.root.context
            task.assignees.forEach { user ->
                val imageView = ImageView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(80, 80)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    val margin = 8
                    setPadding(margin, margin, margin, margin)
                    if (!user.profileImageUrl.isNullOrEmpty()) {
                        Glide.with(context).load(user.profileImageUrl).circleCrop().into(this)
                    } else {
                        setImageResource(R.drawable.ic_profile_placeholder)
                    }
                }
                binding.assignedUsersContainer.addView(imageView)
            }

            // 📷 Görsel Gösterimi
            if (!task.imageUrl.isNullOrEmpty()) {
                binding.imgTaskImage.visibility = View.VISIBLE
                Glide.with(binding.root.context)
                    .load(task.imageUrl)
                    .into(binding.imgTaskImage)
            } else {
                binding.imgTaskImage.visibility = View.GONE
            }

            // 🎙 Sesli Mesaj
            if (!task.audioUrl.isNullOrEmpty()) {
                binding.voiceMessageLayout.visibility = View.VISIBLE
                setupAudioPlayer(task.audioUrl!!)
            } else {
                binding.voiceMessageLayout.visibility = View.GONE
            }

            // 📁 Dosya Gösterimi
            if (!task.fileUrl.isNullOrEmpty()) {
                binding.fileLayout.visibility = View.VISIBLE
                binding.tvFileName.text = extractFileName(task.fileUrl)
                binding.fileLayout.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.parse(task.fileUrl), "*/*")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    binding.root.context.startActivity(intent)
                }
            } else {
                binding.fileLayout.visibility = View.GONE
            }

            // ⏳ Geri Sayım
            updateCountdownFromText(formattedDeadline)

            // ⏰ Hatırlatıcı
            binding.btnReminder.setOnClickListener {
                ReminderUtils.showDateTimePicker(it.context, task.id, task.messageText)
                binding.btnReminder.setImageResource(R.drawable.active_alarm)
                Toast.makeText(it.context, "Hatırlatıcı kuruldu: ${task.messageText}", Toast.LENGTH_SHORT).show()
            }
        }

        private fun setupAudioPlayer(audioUrl: String) {
            val waveformView = binding.waveform
            val durationView = binding.tvDuration
            val playPauseBtn = binding.btnPlayPause

            val sample = IntArray(100) { (0..100).random() }
            waveformView.setSampleFrom(sample)

            try {
                val player = MediaPlayer().apply {
                    setDataSource(audioUrl)
                    prepare()
                }

                val duration = player.duration
                val minutes = duration / 1000 / 60
                val seconds = (duration / 1000) % 60
                durationView.text = String.format("%02d:%02d", minutes, seconds)

                playPauseBtn.setOnClickListener {
                    if (isPlaying) {
                        mediaPlayer?.pause()
                        isPlaying = false
                        audioTimer?.cancel()
                        playPauseBtn.setImageResource(R.drawable.ic_play)
                    } else {
                        mediaPlayer?.release()
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(audioUrl)
                            prepare()
                            start()
                        }

                        isPlaying = true
                        playPauseBtn.setImageResource(R.drawable.ic_pause)

                        audioTimer = object : CountDownTimer(duration.toLong(), 1000) {
                            override fun onTick(millisUntilFinished: Long) {
                                val m = millisUntilFinished / 1000 / 60
                                val s = (millisUntilFinished / 1000) % 60
                                durationView.text = String.format("%02d:%02d", m, s)

                                val progress = ((duration - millisUntilFinished).toFloat() / duration.toFloat()) * waveformView.maxProgress
                                waveformView.progress = progress
                            }

                            override fun onFinish() {
                                durationView.text = String.format("%02d:%02d", minutes, seconds)
                                isPlaying = false
                                mediaPlayer?.release()
                                mediaPlayer = null
                                playPauseBtn.setImageResource(R.drawable.ic_play)
                                waveformView.progress = 0f
                            }
                        }.start()
                    }
                }

            } catch (e: IOException) {
                Log.e("AssignedTaskAdapter", "Ses çalınamadı: ${e.message}")
            }
        }

        private fun updateCountdownFromText(deadlineText: String) {
            countDownTimer?.cancel()

            val format = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
            val deadlineDate = format.parse(deadlineText)
            val deadlineMillis = deadlineDate?.time ?: return

            val currentTime = System.currentTimeMillis()
            val remainingTime = deadlineMillis - currentTime

            if (remainingTime > 0) {
                countDownTimer = object : CountDownTimer(remainingTime, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        val hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished)
                        val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60
                        val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60

                        binding.tvCountdown.text = String.format(
                            "%02d:%02d:%02d kaldı",
                            hours, minutes, seconds
                        )
                    }

                    override fun onFinish() {
                        binding.tvCountdown.text = "⏰ Süre doldu"
                        ReminderUtils.showNotification(
                            binding.root.context,
                            "${binding.tvMessage.text} görevinin süresi doldu!"
                        )
                    }
                }.start()
            } else {
                binding.tvCountdown.text = "⏰ Süre doldu"
            }
        }

        private fun extractFileName(url: String): String {
            return Uri.parse(url).lastPathSegment ?: "dosya"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemAssignedTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<AssignedTask>() {
        override fun areItemsTheSame(oldItem: AssignedTask, newItem: AssignedTask): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: AssignedTask, newItem: AssignedTask): Boolean =
            oldItem == newItem
    }
}