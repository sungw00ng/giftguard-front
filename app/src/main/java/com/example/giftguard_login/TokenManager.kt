package com.example.giftguard_login

import android.content.Context

/**
 * JWT 토큰의 저장 및 로드를 전담하는 클래스.
 * UI(Activity)나 네트워크 로직과 독립적으로 동작합니다.
 */
class TokenManager(private val context: Context) {

    // 토큰이 저장될 SharedPreferences 파일 이름
    private val PREFS_NAME = "auth"
    private val JWT_TOKEN_KEY = "jwt_token"

    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 서버로부터 받은 JWT 토큰을 로컬 저장소에 저장합니다.
     */
    fun saveJwtToken(token: String) {
        sharedPreferences.edit().putString(JWT_TOKEN_KEY, token).apply()
    }

    /**
     * 로컬 저장소에 저장된 JWT 토큰을 불러옵니다.
     * 토큰이 없으면 null을 반환합니다.
     */
    fun getJwtToken(): String? {
        return sharedPreferences.getString(JWT_TOKEN_KEY, null)
    }

    /**
     * 저장된 JWT 토큰을 삭제합니다. (로그아웃 기능 시 사용)
     */
    fun deleteJwtToken() {
        sharedPreferences.edit().remove(JWT_TOKEN_KEY).apply()
    }
}