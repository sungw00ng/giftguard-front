package com.example.giftguard_login

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "MyFCM"

    // 🔥 새 채널 ID로 변경 (예전 채널 설정 꼬인 거 무시)
    private val CHANNEL_ID = "giftguard_high_channel_v2"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "🔥 onMessageReceived 호출됨!")
        Log.d(TAG, "from = ${remoteMessage.from}")
        Log.d(TAG, "data = ${remoteMessage.data}")
        Log.d(TAG, "notification = ${remoteMessage.notification}")

        val notif = remoteMessage.notification
        val title = notif?.title ?: "GiftGuard 알림"
        val body  = notif?.body ?: "새 알림이 도착했습니다."

        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        // 1) 채널 생성 (Oreo 이상 필수)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channelName = "GiftGuard 알림 (중요)"
            val desc = "기프티콘 / 지오펜스 관련 푸시 알림"

            val channel = NotificationChannel(
                CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_HIGH   // 🔥 HIGH로 올려버리기
            ).apply {
                description = desc
                enableVibration(true)
            }

            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }

        // 2) 알림 클릭 시 열릴 화면 (프로필 or 메인)
        val intent = Intent(this, ProfileActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntentFlags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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

        // 3) 알림 빌더
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)  // 앱 아이콘
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)   // 🔥 우선순위 높게

        // 4) 실제 알림 띄우기
        val notificationId = System.currentTimeMillis().toInt()
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            Log.d(TAG, "🔔 notify 호출: id=$notificationId, title=$title, body=$body")
            nm.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ 알림 표시 실패 (권한 문제 가능): ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 알림 표시 중 예외 발생: ${e.message}", e)
        }
    }
}
