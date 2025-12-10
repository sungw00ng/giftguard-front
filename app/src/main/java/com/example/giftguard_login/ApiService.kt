package com.example.giftguard_login

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Query
import retrofit2.http.POST
import retrofit2.http.Body



// 서버 JSON 응답 모델
data class TokenResponse(val token: String)
data class ProfileResponse(val name: String, val email: String)

interface ApiService {

    // 모바일 앱: idToken → JWT 받기
    // 서버가 Accept 헤더로 분기한다면 JSON 반환하도록 유도
    @GET("login")
    @Headers("Accept: application/json")
    suspend fun loginWithGoogle(
        @Query("idToken") idToken: String
    ): Response<TokenResponse>

    // JWT로 프로필 얻기
    @GET("user")
    suspend fun fetchProfile(
        @Header("Authorization") authorization: String // "Bearer <token>"
    ): Response<ProfileResponse>

    @POST("/users/fcm-token")
    suspend fun sendFcmToken(
        @Header("Authorization") authorization: String, // "Bearer {JWT}"
        @Body body: FcmTokenRequest
    ): Response<Unit>
}
