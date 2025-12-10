//GeofenceManager.kt
package com.example.giftguard_login

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * 지오펜싱 등록 및 Places 검색을 관리하는 클래스
 */
class GeofenceManager(
    private val context: Context,
    private val authRepository: AuthRepository,
    private val placesClient: PlacesClient
) {
    private val TAG = "GeofenceManager"

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    // 💡 지오펜스 설정 상수: 백그라운드 감지 성공률을 높이기 위해 250m로 확장
    private val GEOFENCE_RADIUS_IN_METERS = 270f
    private val GEOFENCE_REQUEST_CODE = 2609

    // 지오펜싱 PendingIntent 생성
    private val geofencePendingIntent: PendingIntent by lazy { createGeofencePendingIntent() }

    // 흥업면 기준 좌표 (Fallback용)
    private val HEUNGEOP_CENTER = LatLng(37.385, 127.91)

    // 리스너 인터페이스 (MapsActivity로 결과를 전달)
    interface GeofenceListener {
        fun onGeofencesRegistered(locations: List<Triple<String, String, LatLng>>)
        fun onGeofenceSetupFailed(message: String)
    }

    private var listener: GeofenceListener? = null

    fun setListener(listener: GeofenceListener) {
        this.listener = listener
    }

    private fun createGeofencePendingIntent(): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, GEOFENCE_REQUEST_CODE, intent, flags)
    }

    private fun getErrorString(errorCode: Int): String {
        return when (errorCode) {
            GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> "서비스를 사용할 수 없습니다."
            GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> "등록된 지오펜스가 너무 많습니다."
            GeofenceStatusCodes.ERROR, 13 -> "일반 Geofence 오류 발생."
            -1 -> "Unknown Error: Google Play Services 불안정."
            else -> "알 수 없는 오류 코드: $errorCode"
        }
    }

    private fun createGeofenceRequest(geofence: Geofence): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER) // 🔥 ENTER 즉시 트리거
            addGeofence(geofence)
        }.build()
    }


    // ----------------------------------------------------
    // Public API: 지오펜스 등록 시작
    // ----------------------------------------------------

    fun setupGeofences(currentLocationLatLng: LatLng?) {
        // 1) 기존 지오펜스 제거
        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnSuccessListener {
                Log.d(TAG, "📍 기존 지오펜스 제거 완료. 새로 등록 시작.")
                loadGifticonsAndRegisterGeofences(currentLocationLatLng)
            }
            .addOnFailureListener { e ->
                val statusCode = if (e is ApiException) e.statusCode else -1
                val errorMessage = getErrorString(statusCode)
                Log.e(TAG, "⚠️ 기존 지오펜스 제거 실패: $errorMessage (Code: $statusCode)", e)

                listener?.onGeofenceSetupFailed("지오펜스 초기화 실패: $errorMessage.")
            }
    }

    private fun loadGifticonsAndRegisterGeofences(currentLocationLatLng: LatLng?) {
        authRepository.fetchGifticonList { isSuccess, responseBody ->
            if (!isSuccess || responseBody.isNullOrBlank()) {
                Log.w(TAG, "기프티콘 목록 요청 실패 또는 빈 응답: $responseBody")
                listener?.onGeofenceSetupFailed("기프티콘 목록을 불러오지 못했습니다.")
                return@fetchGifticonList
            }

            try {
                val jsonArray = JSONArray(responseBody)
                if (jsonArray.length() == 0) {
                    Log.d(TAG, "저장된 기프티콘이 없습니다.")
                    return@fetchGifticonList
                }

                val unusedGifticons = mutableListOf<Pair<String, String>>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    if (!obj.optBoolean("isUsed", false)) {
                        unusedGifticons.add(obj.optString("gifticonId", "") to obj.optString("storeName", "매장 이름 없음"))
                    }
                }

                if (unusedGifticons.isEmpty()) {
                    Log.d(TAG, "사용 가능한 기프티콘이 없습니다.")
                    return@fetchGifticonList
                }

                // Coroutine Scope 내에서 Places API 비동기 처리
                val scope = CoroutineScope(Dispatchers.IO)
                scope.launch {
                    val gifticonLocations = mutableListOf<Triple<String, String, LatLng>>()
                    val origin = currentLocationLatLng ?: HEUNGEOP_CENTER

                    for ((id, storeName) in unusedGifticons) {
                        val latLngList = findNearbyLatLngByStoreName(storeName, origin)
                        if (latLngList.isNotEmpty()) {
                            val nearest = latLngList.minByOrNull {
                                SphericalUtil.computeDistanceBetween(origin, it)
                            }
                            if (nearest != null) {
                                gifticonLocations.add(Triple(id, storeName, nearest))
                            }
                        }
                    }

                    // UI 스레드에서 결과 처리 및 MapsActivity로 전달
                    Handler(Looper.getMainLooper()).post {
                        processGeofences(gifticonLocations, origin)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "기프티콘 목록 파싱/지오펜스 설정 중 에러", e)
                listener?.onGeofenceSetupFailed("기프티콘 데이터 처리 중 오류가 발생했습니다.")
            }
        }
    }

    private fun processGeofences(gifticonLocations: List<Triple<String, String, LatLng>>, origin: LatLng) {
        if (gifticonLocations.isEmpty()) {
            listener?.onGeofenceSetupFailed("주변에 등록 가능한 매장이 없습니다.")
            return
        }

        // 내 위치에서 가까운 10개 지점만 선택
        val nearest10 = gifticonLocations
            .sortedBy { (_, _, latLng) -> SphericalUtil.computeDistanceBetween(origin, latLng) }
            .take(10)

        Log.d(TAG, "⭐ 최종 지오펜스 등록 대상 수: ${nearest10.size}개")

        nearest10.forEach { (id, storeName, latLng) ->
            addGeofenceForStore(id, storeName, latLng)
        }

        // MapsActivity에게 마커 표시 및 최종 등록된 위치 정보를 전달
        listener?.onGeofencesRegistered(nearest10)
    }

    // ----------------------------------------------------
    // Places API 검색 로직 (suspend 함수)
    // ----------------------------------------------------

    private fun getBoundingBox(center: LatLng, radiusInMeters: Double): RectangularBounds {
        val northEast = SphericalUtil.computeOffset(center, radiusInMeters * Math.sqrt(2.0), 45.0)
        val southWest = SphericalUtil.computeOffset(center, radiusInMeters * Math.sqrt(2.0), 225.0)
        return RectangularBounds.newInstance(southWest, northEast)
    }

    private suspend fun findNearbyLatLngByStoreName(
        storeName: String,
        origin: LatLng
    ): List<LatLng> = suspendCoroutine { continuation ->

        val refinedStoreName = storeName.replace(Regex(" (점|지점|본사|코리아|매장|센터|점포)$"), "").trim()
        val finalQuery = if (refinedStoreName == "메가MGC커피") "메가커피" else refinedStoreName
        val sessionToken = AutocompleteSessionToken.newInstance()

        val requestBuilder = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(sessionToken)
            .setQuery(finalQuery)
            .setCountries(listOf("KR"))
            .setTypeFilter(TypeFilter.ESTABLISHMENT)

        val radiusInMeters = 3000.0
        val bias = getBoundingBox(origin, radiusInMeters)
        requestBuilder.setLocationBias(bias)

        val request = requestBuilder.build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                val predictions = response.autocompletePredictions
                if (predictions.isEmpty()) {
                    continuation.resume(emptyList())
                    return@addOnSuccessListener
                }

                val resultList = mutableListOf<LatLng>()
                var remaining = predictions.size

                for (prediction in predictions) {
                    val placeFields = listOf(Place.Field.LAT_LNG, Place.Field.NAME)
                    val placeRequest = FetchPlaceRequest.builder(prediction.placeId, placeFields).setSessionToken(sessionToken).build()

                    placesClient.fetchPlace(placeRequest)
                        .addOnSuccessListener { placeResponse ->
                            placeResponse.place.latLng?.let { resultList.add(it) }
                            remaining--
                            if (remaining == 0) {
                                continuation.resume(resultList)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Place 조회 실패(${storeName}): ${e.message}")
                            remaining--
                            if (remaining == 0) {
                                continuation.resume(resultList)
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "자동완성 요청 실패(${storeName}): ${e.message}")
                continuation.resume(emptyList())
            }
    }

    // ----------------------------------------------------
    // 지오펜스 최종 등록 로직
    // ----------------------------------------------------

    @SuppressLint("MissingPermission")
    private fun addGeofenceForStore(gifticonId: String, storeName: String, latLng: LatLng) {

        val safeStoreName = storeName.replace(Regex("[^a-zA-Z0-9ㄱ-ㅎㅏ-ㅣ가-힣]"), "")
        val latStr = String.format("%.5f", latLng.latitude).replace(".", "_")
        val lngStr = String.format("%.5f", latLng.longitude).replace(".", "_")

        val geofenceId = "gifticon_${gifticonId}_${safeStoreName}_${latStr}_${lngStr}"


        val geofence = Geofence.Builder()
            .setRequestId(geofenceId)
            .setCircularRegion(latLng.latitude, latLng.longitude, GEOFENCE_RADIUS_IN_METERS)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER) // 🔥 ENTER만!
            // 🔥 즉시 응답을 위한 최소값 설정
            .setNotificationResponsiveness(0) // 최소값: 0 (즉시)
            // .setLoiteringDelay(0) // ENTER만 사용하면 DWELL 필요 없음
            .build()

        geofencingClient.addGeofences(createGeofenceRequest(geofence), geofencePendingIntent)
            .addOnSuccessListener {
                Log.d(TAG, "✅ 지오펜스 등록 성공: $storeName (ID=$geofenceId)")
            }
            .addOnFailureListener { e ->
                val statusCode = if (e is ApiException) e.statusCode else -1
                val errorMessage = getErrorString(statusCode)
                Log.e(
                    TAG,
                    "❌ ${storeName} 지오펜스 등록 실패: $errorMessage (Code: $statusCode)",
                    e
                )
            }
    }
}