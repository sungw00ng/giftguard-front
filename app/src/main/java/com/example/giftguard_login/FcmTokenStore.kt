package com.example.giftguard_login

import android.content.Context

data class FcmTokenRequest(
    val fcmToken: String
)

class FcmTokenStore(context: Context) {

    private val prefs = context.getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit().putString("fcm_token", token).apply()
    }

    fun getToken(): String? = prefs.getString("fcm_token", null)
}

