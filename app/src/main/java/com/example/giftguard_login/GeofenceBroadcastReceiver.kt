package com.example.giftguard_login

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceReceiver"
        private val GEOFENCE_ID_PATTERN = Regex("gifticon_([^_]+)_([^_]+)_.*")
    }

    override fun onReceive(context: Context, intent: Intent) {
        // 🔥 context 는 applicationContext 로 통일
        val appContext = context.applicationContext
        val notificationHelper = NotificationHelper(appContext)

        // 💥 1) 여기서부터 이미 브로드캐스트를 받았다는 “디버그용 알림” 날리기


        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent is null.")
            return
        }

        if (geofencingEvent.hasError()) {
            val errorCode = geofencingEvent.errorCode
            val errorString = GeofenceStatusCodes.getStatusCodeString(errorCode)
            Log.e(TAG, "Geofencing Error: $errorString (Code: $errorCode)")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences

        when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                Log.d(TAG, "지오펜스 진입 (ENTER) 감지")

                val triggeredStores = mutableMapOf<String, String>()

                triggeringGeofences?.forEach { geofence ->
                    val requestId = geofence.requestId
                    val match = GEOFENCE_ID_PATTERN.find(requestId)

                    if (match != null && match.groupValues.size >= 3) {
                        val gifticonId = match.groupValues[1]
                        val storeName = match.groupValues[2]

                        triggeredStores[gifticonId] = storeName
                        Log.d(TAG, "ID 추출 성공: ID=$gifticonId, 매장=$storeName")
                    } else {
                        Log.w(TAG, "ID 추출 실패: RequestId=$requestId")
                    }
                }

                if (triggeredStores.isNotEmpty()) {
                    val uniqueStoreNames = triggeredStores.values.toSet()
                    val uniqueGifticonCount = triggeredStores.size

                    val primaryStoreName = uniqueStoreNames.joinToString(", ")
                    val summary = "총 ${uniqueGifticonCount}개 기프티콘이 있습니다."

                    val notificationId = System.currentTimeMillis().toInt()

                    notificationHelper.sendGeofenceNotification(
                        notificationId,
                        primaryStoreName,
                        summary
                    )
                }
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                Log.d(TAG, "지오펜스 이탈 (EXIT) 감지")
            }
            Geofence.GEOFENCE_TRANSITION_DWELL -> {
                Log.d(TAG, "지오펜스 체류 (DWELL) 감지")
            }
            else -> {
                Log.e(TAG, "알 수 없는 지오펜스 전환 유형: $geofenceTransition")
            }
        }
    }
}
