package com.example.whatsapp.ui.assigntask.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.whatsapp.R
import com.example.whatsapp.data.model.User
import com.example.whatsapp.databinding.ItemUserCircleBinding

class AssigneeAdapter(
    private val onUserClick: (User) -> Unit
) : ListAdapter<User, AssigneeAdapter.AssigneeViewHolder>(DiffCallback()) {

    private val selectedUserIds = mutableSetOf<String>()

    inner class AssigneeViewHolder(val binding: ItemUserCircleBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssigneeViewHolder {
        val binding = ItemUserCircleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AssigneeViewHolder(binding)
    }

    fun getSelectedUserIds(): List<String> = selectedUserIds.toList()

    override fun onBindViewHolder(holder: AssigneeViewHolder, position: Int) {
        val user = getItem(position)
        holder.binding.apply {
            tvName.text = user.name

            Glide.with(imgUser.context)
                .load(user.profileImageUrl)
                .circleCrop()
                .into(imgUser)

            if (selectedUserIds.contains(user.uid)) {
                frameUser.setBackgroundResource(R.drawable.green_border)
            } else {
                frameUser.setBackgroundResource(0)
            }

            root.setOnClickListener {
                if (selectedUserIds.contains(user.uid)) {
                    selectedUserIds.remove(user.uid)
                } else {
                    selectedUserIds.add(user.uid)
                }
                notifyItemChanged(position)
                onUserClick(user)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean =
            oldItem.uid == newItem.uid

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean =
            oldItem == newItem
    }
}