package com.gamma.mobile.utils

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build

object DeviceAuthHelper {

    fun isDeviceSecure(context: Context): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            keyguardManager.isDeviceSecure
        } else {
            keyguardManager.isKeyguardSecure
        }
    }

    fun createConfirmDeviceCredentialIntent(
        context: Context,
        title: String = "Autenticación requerida",
        description: String = "Desbloquee su dispositivo para acceder a las credenciales sensibles"
    ): Intent? {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            keyguardManager.createConfirmDeviceCredentialIntent(title, description)
        } else {
            null
        }
    }
}
