package com.example.giftguard_login

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume

class ImageObserverService : Service() {

    private val TAG = "ImageObserverService"
    private lateinit var notificationManager: NotificationManager

    private lateinit var authRepository: AuthRepository
    private lateinit var tokenManager: TokenManager

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    // OCR 감지 및 포그라운드 관련 상수
    private val NOTIFICATION_ID_OCR_FOREGROUND = 1001
    private val CHANNEL_ID_OCR = "GifticonOCRChannel"
    private val CHANNEL_NAME_OCR = "기프티콘 자동 인식"

    private val CONTENT_URI: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    // 🔥 같은 사진을 여러 번 처리하는 것을 막기 위한 디바운스 변수
    private var lastProcessedUri: String? = null
    private var lastProcessedTime: Long = 0L
    private val DUP_INTERVAL_MS = 5_000L   // 5초 안에 같은 URI 들어오면 무시

    companion object {
        // ✅ 자동 동기화용 상수 (목록 갱신용 브로드캐스트)
        const val ACTION_GIFTICON_LIST_UPDATED = "ACTION_GIFTICON_LIST_UPDATED"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "ImageObserverService started.")

        tokenManager = TokenManager(applicationContext)
        authRepository = AuthRepository(tokenManager)

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        startAsForeground()

        // 최신 이미지 감지 ContentObserver 등록
        contentResolver.registerContentObserver(
            CONTENT_URI,
            true,
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)
                    if (uri == null) return

                    val now = System.currentTimeMillis()
                    val uriString = uri.toString()

                    // 🔥 같은 URI가 짧은 시간(5초) 안에 또 들어오면 중복 처리 방지
                    if (uriString == lastProcessedUri && (now - lastProcessedTime) < DUP_INTERVAL_MS) {
                        Log.d(
                            TAG,
                            "중복 이미지 감지 → 처리 스킵: $uriString (delta=${now - lastProcessedTime}ms)"
                        )
                        return
                    }

                    lastProcessedUri = uriString
                    lastProcessedTime = now

                    Log.d(TAG, "새로운 이미지 감지됨(처리 예정): $uri")

                    // 파일 쓰기 완료 대기 (1초 지연) 후 바로 OCR 실행
                    Handler(Looper.getMainLooper()).postDelayed({
                        serviceScope.launch {
                            runOcrAndSave(uri)
                        }
                    }, 1000)
                }
            }
        )
    }

    private fun startAsForeground() {
        // 💡 OCR 감지 서비스의 알림을 사용
        val notification = NotificationCompat.Builder(this, CHANNEL_ID_OCR)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // ⚠️ 적절한 아이콘으로 교체 필요
            .setContentTitle("기프티콘 감지 서비스 실행 중")
            .setContentText("갤러리 이미지 변경을 감시하고 있습니다.")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID_OCR_FOREGROUND, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        Log.d(TAG, "ImageObserverService stopped.")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 1. OCR 포그라운드 채널
            val ocrChannel = NotificationChannel(
                CHANNEL_ID_OCR,
                CHANNEL_NAME_OCR,
                NotificationManager.IMPORTANCE_LOW // 포그라운드 알림은 중요도 낮게 설정
            )
            notificationManager.createNotificationChannel(ocrChannel)
        }
    }

    // ----------------------------------------------------------------------
    // 기존 OCR 및 저장 로직 (코드 유지)
    // ----------------------------------------------------------------------

    /**
     * 새 사진에 대해 OCR을 실행하고, "기프티콘이라고 판단되는 경우"에만 서버에 저장
     */
    private suspend fun runOcrAndSave(uri: Uri) {
        Log.i(TAG, "OCR 처리 시작: $uri")
        try {
            val image = withContext(Dispatchers.IO) {
                InputImage.fromFilePath(this@ImageObserverService, uri)
            }

            val recognizer = TextRecognition.getClient(
                KoreanTextRecognizerOptions.Builder().build()
            )

            val visionText = recognizer.process(image).await()
            val recognizedText = visionText.text

            Log.d(TAG, "OCR 성공. 인식 전체 텍스트: $recognizedText")

            if (recognizedText.isBlank()) {
                Log.d(TAG, "텍스트 인식 결과가 비어 있어서 저장하지 않음.")
                return
            }

            val storeName = extractStoreName(recognizedText)
            val expiryDate = extractExpiryDate(recognizedText)

            // ❗ 기준: 매장명이 기본값("기프티콘 상품")이거나, 유효기간 실패면 = 기프티콘 아님 → 저장 X
            if (storeName == "기프티콘 상품" || expiryDate == "유효기간 추출 실패") {
                Log.d(
                    TAG,
                    "기프티콘이 아닌 것으로 판단, 저장/알림 생략. storeName=$storeName, expiryDate=$expiryDate"
                )
                return
            }

            // 3. 서버에 기프티콘 등록 요청
            authRepository.createNewGifticon(storeName, expiryDate) { isSuccess, responseBody ->
                if (isSuccess) {
                    showResultNotification("자동 저장 성공", "매장: $storeName, 유효기간: $expiryDate")

                    // ✅ 서버 등록 성공 시: UI 갱신을 위한 브로드캐스트 전송
                    sendListUpdateBroadcast()

                } else {
                    Log.e(TAG, "서버 등록 실패: $responseBody")
                    showResultNotification("자동 저장 실패", "서버 등록 중 오류 발생.")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "이미지 접근/처리 또는 OCR 실패: ${e.message}", e)
            showResultNotification("자동 저장 실패", "처리 중 오류 발생: ${e.message?.take(50)}")
        }
    }

    /**
     * ✅ 목록 갱신을 위한 로컬 브로드캐스트 전송 헬퍼 함수
     */
    private fun sendListUpdateBroadcast() {
        Log.d(TAG, "🟢 목록 갱신 브로드캐스트 전송 중: $ACTION_GIFTICON_LIST_UPDATED")
        val intent = Intent(ACTION_GIFTICON_LIST_UPDATED)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    // ----------------------------------------------------
    // OCR 추출 헬퍼 함수 (기존 코드 유지)
    // ----------------------------------------------------

    private fun extractStoreName(text: String): String {
        // ... (기존 코드 유지)
        val lines = text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val exchangeLine = lines.firstOrNull { it.contains("교환처") }
        if (exchangeLine != null) {
            val cleaned = exchangeLine
                .replace("교환처", "")
                .replace(":", "")
                .trim()

            if (cleaned.isNotEmpty()) {
                return cleaned.take(20)
            }
        }

        val brandKeywords = listOf(
            "스타", "이디야", "메가", "커피", "빽다방",
            "던킨", "배스킨", "파리바게뜨", "투썸", "할리스"
        )

        val brandLine = lines.firstOrNull { line ->
            brandKeywords.any { kw -> line.contains(kw) }
        }
        if (brandLine != null) {
            return brandLine.take(20)
        }

        val longest = lines.maxByOrNull { it.length }
        if (longest != null && longest.length >= 3) {
            return longest.take(20)
        }

        return "기프티콘 상품"
    }

    private fun extractExpiryDate(text: String): String {
        // ... (기존 코드 유지)
        val dateRegex = Regex(
            "(\\d{4}[\\.\\-/년])(\\d{1,2}[\\.\\-/월])(\\d{1,2}[\\.\\-/일]?)"
        )

        val matches = dateRegex.findAll(text).toList()
        if (matches.isEmpty()) {
            return "유효기간 추출 실패"
        }

        val inputFormat = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)
        val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)

        val dates = matches.mapNotNull { match ->
            val raw = match.value
                .replace("년", ".")
                .replace("월", ".")
                .replace("일", "")
                .replace("/", ".")
                .replace("-", ".")
                .replace(" ", "")

            try {
                inputFormat.parse(raw)
            } catch (e: Exception) {
                Log.e(TAG, "날짜 파싱 실패: $raw", e)
                null
            }
        }

        if (dates.isEmpty()) {
            return "유효기간 추출 실패"
        }

        val maxDate = dates.maxOrNull()!!
        return outputFormat.format(maxDate)
    }

    private fun extractGiftCode(text: String): String {
        val regex = Regex("(\\w{4}[-\\s]?){2}\\w{4}")
        return regex.find(text)?.value?.replace(Regex("[^a-zA-Z0-9]"), "") ?: "코드 추출 실패"
    }

    // ----------------------------------------------------
    // ✅ Task.await() 확장 함수 (기존 코드 유지)
    // ----------------------------------------------------
    private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T {
        return suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { result ->
                continuation.resume(result)
            }
            addOnFailureListener { exception ->
                if (continuation.isActive) {
                    continuation.resumeWith(Result.failure(exception))
                }
            }
            addOnCanceledListener {
                if (continuation.isActive) {
                    continuation.cancel()
                }
            }
        }
    }

    private fun showResultNotification(title: String, content: String) {
        val resultNotification = NotificationCompat.Builder(this, CHANNEL_ID_OCR) // OCR 채널 사용
            .setSmallIcon(R.drawable.ic_launcher_foreground) // ⚠️ 적절한 아이콘으로 교체 필요
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_OCR_FOREGROUND + 3, resultNotification)
    }
}