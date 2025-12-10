// BackgroundLocationService.kt
package com.example.giftguard_login

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*

/**
 * 백그라운드에서 위치 업데이트를 요청하고 지오펜싱 감지에 사용하기 위한 포그라운드 서비스입니다.
 */
class BackgroundLocationService : Service() {

    private val TAG = "LocationService"
    private val CHANNEL_ID = "background_location_channel"
    private val NOTIFICATION_ID = 1002

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    // ------------------ Service Lifecycle ------------------

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "백그라운드 위치 서비스 생성 및 초기화")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // onCreate에서 주요 설정 함수들을 호출합니다.
        createNotificationChannel() // 채널을 먼저 생성
        createLocationRequest()
        createLocationCallback()
        startForegroundService() // 포그라운드 시작 (내부에서 startLocationUpdates 호출)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "백그라운드 위치 서비스 시작 명령 수신")
        // 서비스가 강제 종료되었을 때 시스템이 서비스를 다시 시작하도록 합니다.
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        Log.d(TAG, "백그라운드 위치 서비스 종료")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ------------------ Location Logic ------------------

    /**
     * LocationRequest 객체를 생성하여 위치 업데이트 빈도 및 정확도를 설정합니다.
     */
    private fun createLocationRequest() {
        // LocationRequest.Builder는 Google Play Services 17.0.0+ 에서 사용됩니다.
        // LocationRequest.create() 대신 Builder를 사용합니다.
        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, // 정확도 설정
            10000L // 10초 간격
        )
            .setMinUpdateIntervalMillis(5000L) // 최소 5초 간격
            .apply {
                // Android 12 (API 31/S) 이상에서 백그라운드 위치 정밀도 개선 옵션
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setWaitForAccurateLocation(true)
                }
            }
            .build()
        Log.d(TAG, "위치 요청 객체 생성 완료")
    }

    /**
     * 위치 업데이트 결과를 처리할 LocationCallback 객체를 생성합니다.
     */
    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                // super.onLocationResult(locationResult)
                // 불필요한 super 호출 제거 (코틀린에서 인터페이스 구현 시)
                val location = locationResult.lastLocation
                if (location != null) {
                    // 로그에 정확도 정보 추가
                    Log.d(
                        TAG,
                        "📍 백그라운드 위치 업데이트: Lat=${location.latitude}, Lon=${location.longitude}, Acc=${location.accuracy}m"
                    )
                    // TODO: 이 위치 정보를 사용하여 지오펜스 감지 로직을 처리합니다.
                }
            }
        }
        Log.d(TAG, "위치 콜백 객체 생성 완료")
    }

    /**
     * FusedLocationProviderClient를 사용하여 위치 업데이트를 시작합니다.
     * Android 13+ (API 33/TIRAMISU)에서는 런타임에 POST_NOTIFICATIONS 권한이 필요할 수 있습니다.
     */
    // startForegroundService()에서 호출되므로 @RequiresPermission 어노테이션을 유지합니다.
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun startLocationUpdates() {
        // 권한 체크는 서비스 호출 전에 이루어져야 하지만, 안전을 위해 다시 확인합니다.
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper() // 메인 루퍼 대신, 별도의 핸들러 스레드를 사용할 수도 있습니다.
            )
            Log.d(TAG, "✅ FusedLocationClient에 위치 업데이트 요청 시작됨")
        } else {
            Log.e(TAG, "❌ 백그라운드 위치 권한 (ACCESS_FINE_LOCATION)이 부여되지 않았습니다.")
            // 권한이 없으면 위치 업데이트를 시작할 수 없습니다. 서비스 중단 고려
            stopSelf()
        }
    }

    /**
     * 위치 업데이트를 중지하고 FusedLocationProviderClient에서 콜백을 제거합니다.
     */
    private fun stopLocationUpdates() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            Log.d(TAG, "위치 업데이트가 FusedLocationClient에서 성공적으로 제거됨")
        } catch (e: Exception) {
            Log.e(TAG, "위치 업데이트 제거 실패: ${e.message}")
        }
    }

    // ------------------ Foreground Service Logic ------------------

    /**
     * 포그라운드 서비스로 시작하고 알림을 표시합니다.
     */
    private fun startForegroundService() {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)

        // 포그라운드 서비스 시작 후 위치 업데이트를 시작합니다.
        startLocationUpdates()
    }

    /**
     * 포그라운드 서비스 알림 객체를 생성합니다.
     */
    private fun buildNotification(): Notification {
        // 알림 클릭 시 MapsActivity로 이동할 PendingIntent 생성
        val intent = Intent(this, MapsActivity::class.java)
        // FLAG_IMMUTABLE은 API 23 이상에서 권장되며, S(31) 이상에서 필수입니다.
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            pendingIntentFlags
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GiftGuard 위치 서비스")
            .setContentText("백그라운드에서 지오펜스 감지 중...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // 사용자가 스와이프하여 제거할 수 없도록 설정
            .build()
    }

    /**
     * Android O (API 26) 이상에서 필요한 알림 채널을 생성합니다.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "백그라운드 위치 서비스 알림",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "백그라운드에서 지오펜스 감지를 위한 위치 업데이트 알림"
                setShowBadge(false) // 앱 아이콘에 알림 배지 표시 안 함
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
            Log.d(TAG, "알림 채널 생성 완료")
        }
    }
}