package com.example.whatsapp.utils.helper

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide

object ImageLoader {
    fun loadCircleImage(context: Context, url: String?, imageView: ImageView, placeholder: Int) {
        if (!url.isNullOrEmpty()) {
            Glide.with(context)
                .load(url)
                .circleCrop()
                .placeholder(placeholder)
                .into(imageView)
        }
    }
}
