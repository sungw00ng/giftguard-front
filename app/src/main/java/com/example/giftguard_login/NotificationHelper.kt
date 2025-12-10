package com.example.giftguard_login

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationHelper(private val context: Context) {

    private val CHANNEL_ID = "GIFTGUARD_GEOFENCE_CHANNEL"
    private val CHANNEL_NAME = "기프티콘 위치 알림"

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    /**
     * 알림 채널을 생성합니다. (Android 8.0/Oreo 이상 필수)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // 높은 중요도로 설정
            ).apply {
                description = "지오펜스 지역 진입 시 미사용 기프티콘 알림을 제공합니다."
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 알림을 띄웁니다.
     * @param notificationId 각 알림을 구분하기 위한 고유 ID
     * @param storeName 알림 제목에 들어갈 매장 이름
     * @param summary 알림 내용에 들어갈 미사용 기프티콘 요약 정보 (ex: '총 3개')
     */
    fun sendGeofenceNotification(notificationId: Int, storeName: String, summary: String) {

        // 알림 클릭 시 실행될 액티비티 (MapsActivity로 이동)
        val contentIntent = Intent(context, MapsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // 🔔 수정된 부분: PendingIntent 플래그를 최신 표준에 맞게 설정 (IMMUTABLE 필수)
        val pendingIntentFlags =
            PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        PendingIntent.FLAG_IMMUTABLE // API 23 (Marshmallow) 이상
                    } else {
                        0
                    }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            pendingIntentFlags
        )

        // 알림 빌드
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            // 🚨 R.drawable.giftguardtitle 아이콘 리소스 문제로 크래시가 났을 가능성이 있으니, 파일의 유효성을 최종 확인해야 합니다.
            .setSmallIcon(R.drawable.giftguardtitle)

            .setContentTitle("🎁 미사용 기프티콘이 있습니다!")
            .setContentText("현재 ${storeName} 매장 근처입니다. ${summary}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent) // 클릭 시 이동
            .setAutoCancel(true) // 클릭 시 알림 제거
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("현재 ${storeName} 매장 근처입니다. ${summary}")
            )
            .build()

        notificationManager.notify(notificationId, notification)
    }
}