package com.example.giftguard_login

import android.content.Context
import com.google.android.libraries.places.api.Places
import java.util.Locale

object PlacesUtil {

    // 🔑 매니페스트에 쓰고 있는 Maps / Places API 키와 동일하게 맞춤
    private const val MAPS_API_KEY = "AIzaSyAX-KKZDUSHTBDAUFAdPCZ1rWU_Bw0F_pU"

    /**
     * 앱 어디서든 부를 수 있는 공용 초기화 함수.
     * 이미 초기화 되어 있으면 아무 일도 안 함.
     */
    fun ensureInitialized(context: Context) {
        if (!Places.isInitialized()) {
            Places.initialize(
                context.applicationContext,
                MAPS_API_KEY,
                Locale.KOREA
            )
        }
    }
}
