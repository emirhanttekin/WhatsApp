package com.example.whatsapp.utils.helper

import android.app.AlertDialog
import android.content.Context
import androidx.fragment.app.FragmentManager
import com.example.whatsapp.data.model.Message
import com.example.whatsapp.ui.assigntask.AssignTaskBottomSheet

object MessageOptionHelper {
    fun showOptions(context: Context, message: Message, groupId: String, fragmentManager: FragmentManager) {
        val options = arrayOf("📌 Mesajı Sabitle", "📅 Görev Ata", "❌ İptal")
        AlertDialog.Builder(context)
            .setTitle("Mesaj Seçenekleri")
            .setItems(options) { dialog, which ->
                when (which) {
                    1 -> {
                        val bottomSheet = AssignTaskBottomSheet(message, groupId)
                        bottomSheet.show(fragmentManager, "AssignTaskBottomSheet")
                    }
                    else -> dialog.dismiss()
                }
            }
            .show()
    }
}
