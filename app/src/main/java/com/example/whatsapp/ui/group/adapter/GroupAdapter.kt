package com.example.whatsapp.ui.group.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.whatsapp.data.model.Group
import com.example.whatsapp.databinding.ItemGroupBinding

class GroupAdapter(
    private var groupList: MutableList<Group>, // ✅ Varsayılan boş liste verdik
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

            // 🔹 Unread mesaj varsa göster, yoksa gizle
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
