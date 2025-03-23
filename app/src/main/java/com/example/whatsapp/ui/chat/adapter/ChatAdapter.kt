package com.example.whatsapp.ui.chat.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.media.MediaPlayer
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.whatsapp.R
import com.example.whatsapp.data.model.Message
import com.example.whatsapp.databinding.ItemMessageReceivedBinding
import com.example.whatsapp.databinding.ItemMessageSentBinding
import com.google.firebase.auth.FirebaseAuth
import android.text.format.DateFormat
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import com.google.firebase.Timestamp
import java.io.IOException
import javax.inject.Inject
import kotlin.math.abs

class ChatAdapter @Inject constructor() : ListAdapter<Message, RecyclerView.ViewHolder>(DiffCallback()) {

    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val SENT_MESSAGE = 1
        private const val RECEIVED_MESSAGE = 2

        fun formatTimestamp(timestamp: Timestamp?): String {
            return if (timestamp != null) {
                val date = timestamp.toDate()
                DateFormat.format("HH:mm", date).toString()
            } else {
                ""
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).senderId == auth.currentUser?.uid) SENT_MESSAGE else RECEIVED_MESSAGE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == SENT_MESSAGE) {
            SentMessageViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_message_sent, parent, false)
            )
        } else {
            ReceivedMessageViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_message_received, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        Log.d("ChatAdapter", "\uD83D\uDCCC Güncellenen Mesaj: ${message.message}")

        if (holder is SentMessageViewHolder) {
            holder.bind(message)
        } else if (holder is ReceivedMessageViewHolder) {
            holder.bind(message)
        }
    }

    class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding = ItemMessageSentBinding.bind(itemView)
        private var mediaPlayer: MediaPlayer? = null
        private var isPlaying = false
        private var countdownTimer: CountDownTimer? = null

        fun bind(message: Message) {
            val isAudioMessage = !message.audioUrl.isNullOrEmpty() && message.audioUrl != "null"
            val isImageMessage = !message.imageUrl.isNullOrEmpty() && message.imageUrl != "null" && message.message != "[Sesli mesaj]"

            when {
                isAudioMessage -> {
                    binding.tvMessageSent.visibility = View.GONE
                    binding.imgMessageSent.visibility = View.GONE
                    binding.voiceMessageLayoutSent.visibility = View.VISIBLE
                    setupAudioPlayer(message.audioUrl ?: "", binding.tvDurationSent, binding.btnPlayPauseSent)
                }

                isImageMessage -> {
                    binding.tvMessageSent.visibility = View.GONE
                    binding.imgMessageSent.visibility = View.VISIBLE
                    binding.voiceMessageLayoutSent.visibility = View.GONE

                    Glide.with(itemView.context)
                        .load(message.imageUrl)
                        .into(binding.imgMessageSent)
                }

                else -> {
                    binding.tvMessageSent.visibility = View.VISIBLE
                    binding.tvMessageSent.text = message.message
                    binding.imgMessageSent.visibility = View.GONE
                    binding.voiceMessageLayoutSent.visibility = View.GONE
                }
            }

            binding.tvMessageTimeSent.text = ChatAdapter.formatTimestamp(message.timestamp)

            Glide.with(itemView.context)
                .load(message.senderProfileImageUrl)
                .circleCrop()
                .into(binding.imgProfileSent)
        }

        private fun setupAudioPlayer(audioUrl: String, durationView: View, playPauseBtn: View) {
            val durationTextView = durationView as? TextView ?: return
            val playPauseImage = playPauseBtn as? ImageView ?: return
            var originalDuration = 0

            try {
                val player = MediaPlayer().apply {
                    setDataSource(audioUrl)
                    prepare()
                }

                originalDuration = player.duration
                val minutes = originalDuration / 1000 / 60
                val seconds = (originalDuration / 1000) % 60
                durationTextView.text = String.format("%02d:%02d", minutes, seconds)

                playPauseBtn.setOnClickListener {
                    if (isPlaying) {
                        mediaPlayer?.pause()
                        isPlaying = false
                        countdownTimer?.cancel()
                        durationTextView.text = String.format("%02d:%02d", minutes, seconds)
                        playPauseImage.setImageResource(R.drawable.ic_play)
                    } else {
                        mediaPlayer?.release()
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(audioUrl)
                            prepare()
                            start()
                        }

                        isPlaying = true
                        playPauseImage.setImageResource(R.drawable.ic_pause)

                        countdownTimer?.cancel()
                        countdownTimer = object : CountDownTimer(originalDuration.toLong(), 1000) {
                            override fun onTick(millisUntilFinished: Long) {
                                val m = millisUntilFinished / 1000 / 60
                                val s = (millisUntilFinished / 1000) % 60
                                durationTextView.text = String.format("%02d:%02d", m, s)
                            }

                            override fun onFinish() {
                                durationTextView.text = String.format("%02d:%02d", minutes, seconds)
                                isPlaying = false
                                mediaPlayer?.release()
                                mediaPlayer = null
                                playPauseImage.setImageResource(R.drawable.ic_play)
                            }
                        }.start()
                    }
                }
            } catch (e: IOException) {
                Log.e("ChatAdapter", "MediaPlayer hatası: ${e.message}")
            }
        }
    }



    class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding = ItemMessageReceivedBinding.bind(itemView)
        private var mediaPlayer: MediaPlayer? = null
        private var isPlaying = false
        private var countdownTimer: CountDownTimer? = null

        fun bind(message: Message) {
            val isAudioMessage = !message.audioUrl.isNullOrEmpty() && message.audioUrl != "null"
            val isImageMessage = !message.imageUrl.isNullOrEmpty() && message.imageUrl != "null" && message.message != "[Sesli mesaj]"

            when {
                isAudioMessage -> {
                    binding.tvMessageReceived.visibility = View.GONE
                    binding.imgMessageReceived.visibility = View.GONE
                    binding.voiceMessageLayoutReceived.visibility = View.VISIBLE
                    setupAudioPlayer(message.audioUrl ?: "", binding.tvDurationReceived, binding.btnPlayPauseReceived)
                }

                isImageMessage -> {
                    binding.tvMessageReceived.visibility = View.GONE
                    binding.imgMessageReceived.visibility = View.VISIBLE
                    binding.voiceMessageLayoutReceived.visibility = View.GONE

                    Glide.with(itemView.context)
                        .load(message.imageUrl)
                        .into(binding.imgMessageReceived)
                }

                else -> {
                    binding.tvMessageReceived.visibility = View.VISIBLE
                    binding.tvMessageReceived.text = message.message
                    binding.imgMessageReceived.visibility = View.GONE
                    binding.voiceMessageLayoutReceived.visibility = View.GONE
                }
            }

            binding.tvSenderName.text = message.senderName
            binding.tvMessageTimeReceived.text = ChatAdapter.formatTimestamp(message.timestamp)

            Glide.with(itemView.context)
                .load(message.senderProfileImageUrl)
                .circleCrop()
                .into(binding.imgProfileReceived)

            val userColor = getUserColor(message.senderId)
            binding.messageBubble.backgroundTintList = ColorStateList.valueOf(userColor)
        }

        private fun setupAudioPlayer(audioUrl: String, durationView: View, playPauseBtn: View) {
            val durationTextView = durationView as? TextView ?: return
            val playPauseImage = playPauseBtn as? ImageView ?: return
            var originalDuration = 0

            try {
                val player = MediaPlayer().apply {
                    setDataSource(audioUrl)
                    prepare()
                }

                originalDuration = player.duration
                val minutes = originalDuration / 1000 / 60
                val seconds = (originalDuration / 1000) % 60
                durationTextView.text = String.format("%02d:%02d", minutes, seconds)

                playPauseBtn.setOnClickListener {
                    if (isPlaying) {
                        mediaPlayer?.pause()
                        isPlaying = false
                        countdownTimer?.cancel()
                        durationTextView.text = String.format("%02d:%02d", minutes, seconds)
                        playPauseImage.setImageResource(R.drawable.ic_play)
                    } else {
                        mediaPlayer?.release()
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(audioUrl)
                            prepare()
                            start()
                        }

                        isPlaying = true
                        playPauseImage.setImageResource(R.drawable.ic_pause)

                        countdownTimer?.cancel()
                        countdownTimer = object : CountDownTimer(originalDuration.toLong(), 1000) {
                            override fun onTick(millisUntilFinished: Long) {
                                val m = millisUntilFinished / 1000 / 60
                                val s = (millisUntilFinished / 1000) % 60
                                durationTextView.text = String.format("%02d:%02d", m, s)
                            }

                            override fun onFinish() {
                                durationTextView.text = String.format("%02d:%02d", minutes, seconds)
                                isPlaying = false
                                mediaPlayer?.release()
                                mediaPlayer = null
                                playPauseImage.setImageResource(R.drawable.ic_play)
                            }
                        }.start()
                    }
                }
            } catch (e: IOException) {
                Log.e("ChatAdapter", "MediaPlayer hatası: ${e.message}")
            }
        }

        private fun getUserColor(userId: String): Int {
            val colors = listOf(
                Color.parseColor("#FFCDD2"),
                Color.parseColor("#F8BBD0"),
                Color.parseColor("#E1BEE7"),
                Color.parseColor("#D1C4E9"),
                Color.parseColor("#BBDEFB"),
                Color.parseColor("#B2DFDB"),
                Color.parseColor("#C8E6C9"),
                Color.parseColor("#DCEDC8"),
                Color.parseColor("#FFF9C4"),
                Color.parseColor("#FFECB3")
            )

            val hash = userId.hashCode()
            val index = abs(hash) % colors.size
            return colors[index]
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}