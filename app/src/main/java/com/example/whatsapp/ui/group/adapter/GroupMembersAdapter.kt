package com.example.whatsapp.ui.group.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.whatsapp.data.model.User
import com.example.whatsapp.databinding.ItemGroupMemberBinding

class GroupMembersAdapter(private val ownerId: String) : RecyclerView.Adapter<GroupMembersAdapter.MemberViewHolder>() {

    private val membersList = mutableListOf<User>()

    fun submitList(newList: List<User>) {
        membersList.clear()
        membersList.addAll(newList)
        notifyDataSetChanged()
    }

    inner class MemberViewHolder(private val binding: ItemGroupMemberBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.tvMemberName.text = user.name
            binding.tvMemberRole.text = if (user.uid == ownerId) "Yönetici" else "Üye"


            val roleColor = if (user.uid == ownerId) Color.RED else Color.BLACK
            binding.tvMemberRole.setTextColor(roleColor)

            Glide.with(binding.root.context)
                .load(user.profileImageUrl)
                .circleCrop() // ✅ Oval hale getirme
                .placeholder(com.example.whatsapp.R.drawable.ic_profile_placeholder)
                .into(binding.ivMemberImage)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemGroupMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(membersList[position])
    }

    override fun getItemCount(): Int = membersList.size
}
