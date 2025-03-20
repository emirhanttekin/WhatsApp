package com.example.whatsapp.ui.chat.adapter

import android.content.res.ColorStateList
import android.graphics.Color
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
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.Timestamp
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
        Log.d("ChatAdapter", "📌 Güncellenen Mesaj: ${message.message}")

        if (holder is SentMessageViewHolder) {
            holder.bind(message)
        } else if (holder is ReceivedMessageViewHolder) {
            holder.bind(message)
        }
    }

    class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding = ItemMessageSentBinding.bind(itemView)

        fun bind(message: Message) {

            val isImageMessage = !message.imageUrl.isNullOrEmpty() && message.imageUrl != "null"

            if (isImageMessage) {

                binding.tvMessageSent.visibility = View.GONE
                binding.imgMessageSent.visibility = View.VISIBLE

                Glide.with(itemView.context)
                    .load(message.imageUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(binding.imgMessageSent)
            } else {

                binding.tvMessageSent.visibility = View.VISIBLE
                binding.tvMessageSent.text = message.message
                binding.imgMessageSent.visibility = View.GONE
            }

            binding.tvMessageTimeSent.text = ChatAdapter.formatTimestamp(message.timestamp)

            val profileImageUrl = if (!message.senderProfileImageUrl.isNullOrEmpty()) message.senderProfileImageUrl
            else "https://example.com/default_avatar.png"

            Glide.with(itemView.context)
                .load(profileImageUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(binding.imgProfileSent)
        }
    }

    class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding = ItemMessageReceivedBinding.bind(itemView)

        fun bind(message: Message) {
            val isImageMessage = !message.imageUrl.isNullOrEmpty() && message.imageUrl != "null"

            if (isImageMessage) {
                binding.tvMessageReceived.visibility = View.GONE
                binding.imgMessageReceived.visibility = View.VISIBLE

                Glide.with(itemView.context)
                    .load(message.imageUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(binding.imgMessageReceived)
            } else {
                binding.tvMessageReceived.visibility = View.VISIBLE
                binding.tvMessageReceived.text = message.message
                binding.imgMessageReceived.visibility = View.GONE
            }

            binding.tvSenderName.visibility = View.VISIBLE
            binding.tvSenderName.text = message.senderName

            Log.d("ChatAdapter", "Gönderen Adı: ${message.senderName}")

            // 📌 Kullanıcıya özel renk ayarla
            val userColor = getUserColor(message.senderId)
            binding.messageBubble.backgroundTintList = ColorStateList.valueOf(userColor)

            binding.tvMessageTimeReceived.text = ChatAdapter.formatTimestamp(message.timestamp)

            val profileImageUrl = if (!message.senderProfileImageUrl.isNullOrEmpty()) message.senderProfileImageUrl
            else "https://example.com/default_avatar.png"

            Glide.with(itemView.context)
                .load(profileImageUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(binding.imgProfileReceived)
        }

        // 📌 Kullanıcıya özel renk atama fonksiyonu
        private fun getUserColor(userId: String): Int {
            val colors = listOf(
                Color.parseColor("#FFCDD2"), // Açık Kırmızı
                Color.parseColor("#F8BBD0"), // Açık Pembe
                Color.parseColor("#E1BEE7"), // Açık Mor
                Color.parseColor("#D1C4E9"), // Açık Lila
                Color.parseColor("#BBDEFB"), // Açık Mavi
                Color.parseColor("#B2DFDB"), // Açık Turkuaz
                Color.parseColor("#C8E6C9"), // Açık Yeşil
                Color.parseColor("#DCEDC8"), // Açık Lime
                Color.parseColor("#FFF9C4"), // Açık Sarı
                Color.parseColor("#FFECB3")  // Açık Turuncu
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
