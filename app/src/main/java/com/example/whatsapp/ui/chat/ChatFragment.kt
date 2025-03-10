package com.example.whatsapp.ui.chat

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.whatsapp.R
import com.example.whatsapp.databinding.FragmentChatBinding

class ChatFragment : Fragment(R.layout.fragment_chat) {

    private lateinit var binding: FragmentChatBinding
    private val args: ChatFragmentArgs by navArgs() // ✅ Seçilen grup ID ve adını alıyoruz

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentChatBinding.bind(view)

        val groupId = args.groupId
        val groupName = args.groupName // ✅ Grup adını da aldık

        println("🔹 Seçilen Grup ID: $groupId, Grup Adı: $groupName") // 🔥 Log ekleyerek kontrol et

        // ✅ Davet butonuna tıklanınca InviteUserFragment'a yönlendir
        binding.btnInvite.setOnClickListener {
            val action = ChatFragmentDirections
                .actionChatFragmentToInviteUserFragment(
                    groupId,
                    groupName
                ) // 🔥 Grup ID ve adını gönder
            findNavController().navigate(action)
        }


    }
}
