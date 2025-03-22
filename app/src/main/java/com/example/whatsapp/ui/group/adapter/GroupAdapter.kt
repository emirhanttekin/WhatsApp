// GroupAdapter.kt
package com.example.whatsapp.ui.group.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.whatsapp.R
import com.example.whatsapp.data.model.Group
import com.example.whatsapp.databinding.ItemGroupBinding

class GroupAdapter(
    private var groupList: MutableList<Group>,
    private val onItemClick: (Group) -> Unit
) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    fun submitList(newList: List<Group>) {
        groupList.clear()
        groupList.addAll(newList)
        notifyDataSetChanged()
    }

    inner class GroupViewHolder(private val binding: ItemGroupBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(group: Group) {
            binding.tvGroupName.text = group.name

            Glide.with(binding.root.context)
                .load(group.imageUrl ?: R.drawable.ic_group_placeholder)
                .circleCrop()
                .into(binding.imgGroup)

            if (group.unreadCount > 0) {
                binding.tvUnreadMessageCount.visibility = View.VISIBLE
                binding.tvUnreadMessageCount.text = group.unreadCount.toString()
            } else {
                binding.tvUnreadMessageCount.visibility = View.GONE
            }

            binding.root.setOnClickListener { onItemClick(group) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(groupList[position])
    }

    override fun getItemCount(): Int = groupList.size
}
