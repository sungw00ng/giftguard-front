package com.example.giftguard_login

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Callback
import okhttp3.Call
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

/**
 * 서버와의 인증 및 데이터 통신을 전담하는 클래스.
 * 모든 네트워크 호출은 OkHttp의 비동기 enqueue를 사용하여 메인 스레드를 블록하지 않습니다.
 */
class AuthRepository(private val tokenManager: TokenManager) {

    private val TAG = "AuthRepository"
    private val client = OkHttpClient()
    // 서버 기본 URL
    private val BASE_URL = "https://unijugate-unaccessible-lorean.ngrok-free.dev"
    private val JSON = "application/json; charset=utf-8".toMediaType()


    // ----------------------------------------------------------------------
    // 인증 및 조회 기능 (모두 비동기 enqueue 사용)
    // ----------------------------------------------------------------------

    /**
     * Google ID 토큰을 서버로 보내 JWT를 받고 저장합니다.
     */
    fun exchangeIdToken(idToken: String, onComplete: (Boolean) -> Unit) {
        val json = JSONObject().apply { put("idToken", idToken) }
        val requestBody = json.toString().toRequestBody(JSON)

        val request = Request.Builder()
            .url("$BASE_URL/google")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "❌ ID Token 전송 통신 실패", e)
                onComplete(false)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = try { response.body?.string() } catch (e: Exception) { null }

                if (response.isSuccessful && responseBody != null) {
                    try {
                        val jsonResponse = JSONObject(responseBody)
                        val jwtToken = jsonResponse.getString("token")
                        tokenManager.saveJwtToken(jwtToken)
                        Log.d(TAG, "🔐 JWT 저장 완료. 토큰 확인: ${jwtToken.take(20)}...")
                        onComplete(true)
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ JWT 토큰 파싱 실패", e)
                        onComplete(false)
                    }
                } else {
                    Log.e(TAG, "❌ 서버 응답 실패: ${response.code}. 응답 본문: $responseBody")
                    onComplete(false)
                }
            }
        })

    }

    /**
     * 저장된 JWT 토큰을 사용하여 사용자 정보를 요청합니다.
     */
    fun fetchUserProfile(onComplete: (Boolean, String?) -> Unit) {
        val jwtToken = tokenManager.getJwtToken()
        if (jwtToken == null) {
            Log.e(TAG, "❌ 저장된 JWT 토큰이 없어 프로필 조회를 할 수 없습니다.")
            onComplete(false, null)
            return
        }

        val request = Request.Builder()
            .url("$BASE_URL/users")
            .addHeader("Authorization", "Bearer $jwtToken")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "❌ 내정보 요청 통신 실패", e)
                onComplete(false, null)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = try { response.body?.string() } catch (e: Exception) { null }

                Log.d(TAG, "👤 내정보 응답코드: ${response.code}")

                if (response.isSuccessful) {
                    onComplete(true, responseBody)
                } else {
                    onComplete(false, responseBody)
                }
            }
        })
    }

    /**
     * 저장된 JWT 토큰을 사용하여 기프티콘 목록을 요청합니다.
     */
    fun fetchGifticonList(onComplete: (Boolean, String?) -> Unit) {
        val jwtToken = tokenManager.getJwtToken()
        if (jwtToken == null) {
            Log.e(TAG, "❌ 저장된 JWT 토큰이 없어 기프티콘 목록 조회를 할 수 없습니다.")
            onComplete(false, null)
            return
        }

        val request = Request.Builder()
            .url("$BASE_URL/gifticons")
            .addHeader("Authorization", "Bearer $jwtToken")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "❌ 기프티콘 목록 요청 통신 실패", e)
                onComplete(false, null)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = try { response.body?.string() } catch (e: Exception) { null }

                Log.d(TAG, "🎁 기프티콘 목록 응답코드: ${response.code}")

                if (response.isSuccessful) {
                    onComplete(true, responseBody)
                } else {
                    Log.e(TAG, "❌ 기프티콘 목록 서버 응답 실패: ${response.code}. 응답 본문: $responseBody")
                    onComplete(false, responseBody)
                }
            }
        })
    }

    // ----------------------------------------------------------------------
    // ✅ 새 기프티콘 등록 기능 (OCR 자동 저장)
    // ----------------------------------------------------------------------

    /**
     * 새 기프티콘 정보를 서버에 POST 요청으로 등록합니다.
     * 매장명, 유효기간, 사용 여부(false 고정)만 전송합니다.
     * @param storeName 매장/상품명
     * @param expirationDate 유효기간 (YYYY-MM-DD 형식)
     * @param callback (성공 여부, 응답 본문/오류 메시지)
     */
    fun createNewGifticon(
        storeName: String,
        expirationDate: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val jwtToken = tokenManager.getJwtToken()
        if (jwtToken == null) {
            callback(false, "No authentication token")
            return
        }

        // 서버에 전송할 JSON 데이터 구성: 매장명, 유효기간, 사용 여부(false)만 포함
        val json = JSONObject().apply {
            put("storeName", storeName)
            put("expirationDate", expirationDate)
            put("isUsed", false) // 사용 여부 (새로 등록 시 미사용)
        }
        val requestBody = json.toString().toRequestBody(JSON)

        Log.d(TAG, "🎁 새 기프티콘 요청 바디 (최소 필드): $json")

        val request = Request.Builder()
            .url("$BASE_URL/gifticons") // POST to /gifticons
            .post(requestBody)
            .addHeader("Authorization", "Bearer $jwtToken")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "❌ 새 기프티콘 등록 통신 실패", e)
                callback(false, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = try { response.body?.string() } catch (e: Exception) { null }

                if (response.isSuccessful) {
                    Log.d(TAG, "✅ 새 기프티콘 등록 성공 (Code: ${response.code})")
                    callback(true, responseBody)
                } else {
                    Log.e(TAG, "❌ 새 기프티콘 등록 서버 응답 실패: ${response.code}. 응답 본문: $responseBody")
                    callback(false, responseBody)
                }
            }
        })
    }

    // ----------------------------------------------------------------------
    // 기프티콘 상태 변경 및 삭제 기능
    // ----------------------------------------------------------------------

    /**
     * 특정 기프티콘 ID의 사용 상태를 '사용 완료'로 변경하도록 서버에 PUT 요청합니다.
     */
    fun updateGifticonUsedStatus(gifticon: Gifticon, usedStatus: Boolean, callback: (Boolean, String?) -> Unit) {
        val jwtToken = tokenManager.getJwtToken()
        if (jwtToken == null) {
            callback(false, "No authentication token")
            return
        }

        val url = "$BASE_URL/gifticons/${gifticon.id}"

        val json = JSONObject().apply {
            put("isUsed", usedStatus)
            put("storeName", gifticon.name)
            put("expirationDate", gifticon.expiration)
        }
        val requestBody = json.toString().toRequestBody(JSON)

        val request = Request.Builder()
            .url(url)
            .put(requestBody)
            .addHeader("Authorization", "Bearer $jwtToken")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "❌ 기프티콘 사용 처리 통신 실패 (ID: ${gifticon.id})", e)
                callback(false, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = try { response.body?.string() } catch (e: Exception) { null }

                if (response.isSuccessful) {
                    Log.d(TAG, "✅ 기프티콘 사용 처리 성공 (ID: ${gifticon.id}, Code: ${response.code})")
                    callback(true, responseBody)
                } else if (response.code == 404) {
                    Log.e(TAG, "❌ 기프티콘 사용 처리 실패: 404 Not Found")
                    callback(false, "404 Not Found: 본인 소유가 아니거나 기프티콘 ID 오류")
                } else {
                    Log.e(TAG, "❌ 기프티콘 사용 처리 서버 응답 실패 (ID: ${gifticon.id}): ${response.code}. 응답 본문: $responseBody")
                    callback(false, responseBody)
                }
            }
        })
    }

    /**
     * 특정 기프티콘 ID를 서버에서 삭제합니다.
     */
    fun deleteGifticon(gifticonId: String, callback: (Boolean, String?) -> Unit) {
        val jwtToken = tokenManager.getJwtToken()
        if (jwtToken == null) {
            callback(false, "No authentication token")
            return
        }

        val url = "$BASE_URL/gifticons/$gifticonId"

        val request = Request.Builder()
            .url(url)
            .delete()
            .addHeader("Authorization", "Bearer $jwtToken")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "❌ 기프티콘 삭제 통신 실패 (ID: $gifticonId)", e)
                callback(false, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = try { response.body?.string() } catch (e: Exception) { null }

                if (response.isSuccessful) {
                    Log.d(TAG, "✅ 기프티콘 삭제 성공 (ID: $gifticonId, Code: ${response.code})")
                    callback(true, null)
                } else if (response.code == 404) {
                    Log.e(TAG, "❌ 기프티콘 삭제 실패: 404 Not Found")
                    callback(false, "404 Not Found: 본인 소유가 아니거나 기프티콘 ID 오류")
                } else {
                    Log.e(TAG, "❌ 기프티콘 삭제 서버 응답 실패 (ID: $gifticonId): ${response.code}. 응답 본문: $responseBody")
                    callback(false, responseBody)
                }
            }
        })
    }

    /**
     * FCM 토큰을 서버에 전송합니다.
     */
    fun sendFcmTokenToServer(fcmToken: String, onComplete: (Boolean, String?) -> Unit) {
        val jwtToken = tokenManager.getJwtToken()
        if (jwtToken == null) {
            Log.e(TAG, "❌ JWT 토큰이 없어 FCM 토큰을 전송할 수 없습니다.")
            onComplete(false, "No authentication token")
            return
        }

        // JSON 필드명 'token'
        val json = JSONObject().apply {
            put("token", fcmToken)
        }

        Log.d(TAG, "📨 FCM 토큰 요청 바디: $json")

        val requestBody = json.toString().toRequestBody(JSON)

        val request = Request.Builder()
            .url("$BASE_URL/users/fcm-token")
            .post(requestBody)
            .addHeader("Authorization", "Bearer $jwtToken")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "❌ FCM 토큰 전송 통신 실패", e)
                onComplete(false, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = try { response.body?.string() } catch (e: Exception) { null }

                Log.d(TAG, "📥 FCM 토큰 응답 code=${response.code}, body=$responseBody")

                if (response.isSuccessful) {
                    Log.d(TAG, "✅ FCM 토큰 전송 성공")
                    onComplete(true, responseBody)
                } else {
                    Log.e(
                        TAG,
                        "❌ FCM 토큰 전송 서버 응답 실패: code=${response.code}, body=$responseBody"
                    )
                    onComplete(false, responseBody)
                }
            }
        })
    }

}