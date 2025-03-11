package com.example.whatsapp.ui.chat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.whatsapp.R
import com.example.whatsapp.data.model.Message
import com.example.whatsapp.databinding.ItemMessageReceivedBinding
import com.example.whatsapp.databinding.ItemMessageSentBinding
import com.google.firebase.auth.FirebaseAuth
import android.text.format.DateFormat
import com.google.firebase.Timestamp
import java.util.*
import javax.inject.Inject

class ChatAdapter @Inject constructor() : ListAdapter<Message, RecyclerView.ViewHolder>(DiffCallback()) {

    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val SENT_MESSAGE = 1
        private const val RECEIVED_MESSAGE = 2

        fun formatTimestamp(timestamp: Timestamp?): String {
            return if (timestamp != null) {
                val date = timestamp.toDate() // 🔥 Firestore Timestamp'i Date'e çevir
                val localTime = Calendar.getInstance()
                localTime.time = date
                DateFormat.format("HH:mm", localTime).toString() // 🔥 Kullanıcının lokal saat dilimine çevir
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
        if (holder is SentMessageViewHolder) {
            holder.bind(message)
        } else if (holder is ReceivedMessageViewHolder) {
            holder.bind(message)
        }
    }

    class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding = ItemMessageSentBinding.bind(itemView)
        fun bind(message: Message) {
            binding.tvMessageSent.text = message.message
            binding.tvMessageTimeSent.text = formatTimestamp(message.timestamp)
        }
    }
    class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding = ItemMessageReceivedBinding.bind(itemView)
        fun bind(message: Message) {
            binding.tvMessageReceived.text = message.message
            binding.tvMessageTimeReceived.text = formatTimestamp(message.timestamp)
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
