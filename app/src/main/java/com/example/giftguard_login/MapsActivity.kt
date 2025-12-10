//MapsActivity.kt
package com.example.giftguard_login

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.giftguard_login.databinding.ActivityMapsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient

class MapsActivity : AppCompatActivity(), OnMapReadyCallback,
    LocationManager.LocationListener,
    GeofenceManager.GeofenceListener
{

    private val TAG = "MapsActivity"

    private lateinit var map: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    // GeofenceManager와 일치하는 최종 반경 설정 (250m)
    private val GEOFENCE_VISUAL_RADIUS = 270.0 // meters

    // 매니저 인스턴스
    private lateinit var locationManager: LocationManager
    private lateinit var geofenceManager: GeofenceManager

    // 상태 저장 변수
    private var currentLocationLatLng: LatLng? = null

    // 서버 통신
    private lateinit var tokenManager: TokenManager
    private lateinit var authRepository: AuthRepository

    // Places SDK
    private lateinit var placesClient: PlacesClient

    // 흥업면 기준 좌표 (Fallback용)
    private val HEUNGEOP_CENTER = LatLng(37.385, 127.91)

    // -----------------------------
    // 권한 런처들
    // -----------------------------
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "알림 권한 허용됨.")
            // 🔔 알림 권한 획득 후 서비스 시작
            startBackgroundServices()
        } else {
            Log.w(TAG, "알림 권한 거부됨.")
            Toast.makeText(this, "알림 권한이 없어 기프티콘 자동 감지 알림을 받을 수 없습니다.", Toast.LENGTH_LONG).show()
        }
    }

    private val requestLocationPerms = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val fineLocationGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val backgroundLocationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            result[Manifest.permission.ACCESS_BACKGROUND_LOCATION] == true
        } else true

        if (fineLocationGranted && backgroundLocationGranted) {
            Log.d(TAG, "모든 위치 권한 허용됨. 내 위치 + 지오펜스 세팅 시작.")

            // 🔥 배터리 최적화 확인 추가
            checkBatteryOptimization()

            activateMyLocationAndSetup()
        } else {
            Toast.makeText(this, "⚠️ 지오펜싱을 위해 '항상 허용' 위치 권한이 필요합니다. 설정에서 변경해주세요.", Toast.LENGTH_LONG).show()
            val intent = Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null)
            )
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    // -----------------------------
    // 생명주기
    // -----------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. 초기화 및 의존성 주입
        tokenManager = TokenManager(applicationContext)
        authRepository = AuthRepository(tokenManager)

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyAX-KKZDUSHTBDAUFAdPCZ1rWU_Bw0F_pU")
        }
        placesClient = Places.createClient(this)

        // 매니저 초기화 및 리스너 설정
        locationManager = LocationManager(this).apply {
            setListener(this@MapsActivity)
        }
        geofenceManager = GeofenceManager(this, authRepository, placesClient).apply {
            setListener(this@MapsActivity)
        }

        // 알림 권한 확인 및 요청 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // 🔔 알림 권한이 이미 있다면 바로 서비스 시작
                startBackgroundServices()
            }
        } else {
            // 🔔 Android 12 이하에서는 바로 서비스 시작
            startBackgroundServices()
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // 🔥 백그라운드 위치 서비스 시작 (추가)
        startBackgroundLocationService()
    }

    // -----------------------------
    // 🔥 백그라운드 서비스 시작 로직 (수정됨)
    // -----------------------------
    private fun startBackgroundServices() {
        Log.d(TAG, "모든 백그라운드 서비스 시작")

        // 1. ImageObserverService 시작 (OCR 감지)
        startImageObserverService()

        // 2. BackgroundLocationService 시작 (위치 업데이트 유지)
        startBackgroundLocationService()
    }

    private fun startImageObserverService(storeNames: String? = null) {
        Log.d(TAG, "ImageObserverService 시작")
        val serviceIntent = Intent(this, ImageObserverService::class.java)

        if (!storeNames.isNullOrEmpty()) {
            serviceIntent.putExtra("EXTRA_STORE_NAMES", storeNames)
            Log.d(TAG, "서비스에 매장 이름 전달: $storeNames")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    // 🔥 새로운 메서드: 백그라운드 위치 서비스 시작
    private fun startBackgroundLocationService() {
        Log.d(TAG, "백그라운드 위치 서비스 시작 시도")

        val serviceIntent = Intent(this, BackgroundLocationService::class.java)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Log.d(TAG, "백그라운드 위치 서비스 시작 성공")
        } catch (e: Exception) {
            Log.e(TAG, "백그라운드 위치 서비스 시작 실패: ${e.message}")
        }
    }

    // 🔥 새로운 메서드: 배터리 최적화 확인
    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val packageName = packageName
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager

            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                Log.w(TAG, "⚠️ 배터리 최적화가 활성화되어 있어 백그라운드 지오펜스가 작동하지 않을 수 있습니다")

                // 사용자에게 알림 (선택사항)
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "백그라운드 지오펜스를 위해 '설정 > 배터리 > 배터리 최적화'에서 GiftGuard를 '최적화 안함'으로 설정해주세요.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                Log.d(TAG, "✅ 배터리 최적화가 비활성화되어 있음 - 백그라운드 지오펜스 정상 작동 가능")
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isCompassEnabled = true
        map.uiSettings.isMapToolbarEnabled = true
        checkLocationPermissions()
    }

    // -----------------------------
    // 권한 & 내 위치 시작
    // -----------------------------

    private fun checkLocationPermissions() {
        val fineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val backgroundRequired = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        val backgroundGranted = if (backgroundRequired) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else true

        if (fineGranted && backgroundGranted) {
            activateMyLocationAndSetup()
        } else {
            val permissionsToRequest = mutableListOf<String>()
            if (!fineGranted) permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (backgroundRequired && !backgroundGranted) {
                permissionsToRequest.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            requestLocationPerms.launch(permissionsToRequest.toTypedArray())
        }
    }

    @SuppressLint("MissingPermission")
    private fun activateMyLocationAndSetup() {
        if (!::map.isInitialized) return

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
            locationManager.startLocationUpdates()
        } else {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(HEUNGEOP_CENTER, 14f))
            Log.w(TAG, "위치 권한 부족으로 현재 위치 대신 흥업면 중심으로 이동.")
            geofenceManager.setupGeofences(null)
        }
        map.clear()
    }

    // ——————————————
    // LocationManager.LocationListener 구현
    // ——————————————

    override fun onInitialLocationReceived(latLng: LatLng) {
        currentLocationLatLng = latLng
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        geofenceManager.setupGeofences(latLng)
    }

    override fun onLocationUpdateStopped() {
        Log.d(TAG, "위치 매니저로부터 업데이트 중지 알림 받음.")
    }

    // -----------------------------
    // GeofenceManager.GeofenceListener 구현
    // -----------------------------

    override fun onGeofencesRegistered(locations: List<Triple<String, String, LatLng>>) {
        val storeNames = locations.map { it.second }.distinct().joinToString(", ")

        locations.forEach { (_, storeName, latLng) ->
            // 1. 마커 추가
            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(storeName)
                    .snippet("기프티콘 매장 (탐지 반경 ${GEOFENCE_VISUAL_RADIUS.toInt()}m)")
            )

            // 2. 지오펜스 탐지 반경 원(Circle) 추가 (시각화)
            map.addCircle(
                CircleOptions()
                    .center(latLng)
                    .radius(GEOFENCE_VISUAL_RADIUS)
                    .strokeColor(Color.argb(100, 255, 0, 0))
                    .fillColor(Color.argb(30, 255, 0, 0))
                    .strokeWidth(3f)
            )
        }

        if (storeNames.isNotEmpty()) {
            startImageObserverService(storeNames)
        }

        // 🔥 지오펜스 등록 완료 시 알림
        Toast.makeText(this, "✅ 지오펜스 ${locations.size}개 등록 완료! 백그라운드에서도 작동합니다.", Toast.LENGTH_LONG).show()

    }

    override fun onGeofenceSetupFailed(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.e(TAG, "지오펜스 설정 실패: $message")
    }

    // 🔥 새로운 메서드: 디버그 알림
    private fun sendDebugNotification(title: String, message: String) {
        try {
            val notificationHelper = NotificationHelper(applicationContext)
            val notificationId = System.currentTimeMillis().toInt()
            notificationHelper.sendGeofenceNotification(notificationId, title, message)
        } catch (e: Exception) {
            Log.e(TAG, "디버그 알림 전송 실패: ${e.message}")
        }
    }

    // 앱이 포그라운드로 돌아올 때
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "MapsActivity가 포그라운드로 돌아옴")
    }

    // 앱이 백그라운드로 갈 때
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "MapsActivity가 백그라운드로 감")
        Toast.makeText(this, "앱이 백그라운드로 전환됩니다. 지오펜스는 계속 작동합니다.", Toast.LENGTH_SHORT).show()
    }
}