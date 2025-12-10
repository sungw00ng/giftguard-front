package com.example.giftguard_login

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng

/**
 * 위치 업데이트를 관리하는 클래스
 */
class LocationManager(private val context: Context) {

    private val TAG = "LocationManager"

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    // 위치 요청 간격 설정 (기존 MapsActivity에서 가져옴)
    private val UPDATE_INTERVAL_MS: Long = 10000
    private val FASTEST_UPDATE_INTERVAL_MS: Long = 5000

    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    // 첫 위치 수신 성공 여부를 MapsActivity에 전달하기 위한 인터페이스
    interface LocationListener {
        fun onInitialLocationReceived(latLng: LatLng)
        fun onLocationUpdateStopped()
    }

    private var listener: LocationListener? = null

    init {
        createLocationRequest()
    }

    fun setListener(listener: LocationListener) {
        this.listener = listener
        createLocationCallback()
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = UPDATE_INTERVAL_MS
            fastestInterval = FASTEST_UPDATE_INTERVAL_MS
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                val lastLocation = locationResult.lastLocation
                if (lastLocation != null) {
                    val myLatLng = LatLng(lastLocation.latitude, lastLocation.longitude)
                    Log.d(TAG, "📍 위치 수신: $myLatLng")

                    // 첫 위치를 받으면 MapsActivity에 전달하고 업데이트 중지
                    // MapsActivity는 이 위치를 이용해 지도를 움직이고 지오펜스를 설정함.
                    listener?.onInitialLocationReceived(myLatLng)
                    stopLocationUpdates()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "위치 업데이트 요청 시작.")
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper() // MapsActivity의 mainLooper 대신 일반 MainLooper 사용
            )
        } else {
            Log.e(TAG, "위치 권한 없음. 위치 업데이트 시작 불가.")
        }
    }

    fun stopLocationUpdates() {
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            listener?.onLocationUpdateStopped()
            Log.d(TAG, "위치 업데이트 요청 중지됨.")
        }
    }
}