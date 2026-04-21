package com.nice.cataloguevastra.utils

import android.content.Context
import android.os.Build
import android.provider.Settings

object DeviceInfoProvider {

    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ).orEmpty()
    }

    fun getDeviceName(): String {
        return buildString {
            append(Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() })
            if (Build.MODEL.orEmpty().isNotBlank()) {
                append(" ")
                append(Build.MODEL)
            }
        }.trim()
    }
}
