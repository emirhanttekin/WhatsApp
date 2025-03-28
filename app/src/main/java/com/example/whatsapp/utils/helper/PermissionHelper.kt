    package com.example.whatsapp.utils.helper

    import android.content.Context
    import android.content.pm.PackageManager
    import androidx.core.content.ContextCompat

    object PermissionHelper {
        fun  isPermissionGranted(context : Context, permission : String): Boolean {
            return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }