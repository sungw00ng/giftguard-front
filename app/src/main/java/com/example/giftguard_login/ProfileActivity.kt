package com.example.giftguard_login

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager // ✅ LocalBroadcastManager import
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONArray
import org.json.JSONObject

// ----------------------------------------------------------------------
// 🎁 데이터 클래스
// ----------------------------------------------------------------------
data class Gifticon(
    val id: String,
    val name: String,
    val expiration: String,
    val isUsed: Boolean
)

// ----------------------------------------------------------------------
// 🎁 어댑터 클래스
// ----------------------------------------------------------------------
class GifticonAdapter(
    private val gifticonList: MutableList<Gifticon>,
    private val onGifticonLongClicked: (Gifticon) -> Unit
) : RecyclerView.Adapter<GifticonAdapter.GifticonViewHolder>() {

    inner class GifticonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(android.R.id.text1)
        val expiration: TextView = view.findViewById(android.R.id.text2)

        init {
            view.setOnLongClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val gifticon = gifticonList[adapterPosition]
                    onGifticonLongClicked(gifticon)
                    true
                } else {
                    false
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GifticonViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            android.R.layout.simple_list_item_2, parent, false
        )
        return GifticonViewHolder(view)
    }

    override fun onBindViewHolder(holder: GifticonViewHolder, position: Int) {
        val gifticon = gifticonList[position]

        val statusText = if (gifticon.isUsed) " [사용 완료]" else " [미사용]"
        holder.name.text = gifticon.name + statusText
        holder.expiration.text = "만료일: ${gifticon.expiration}"

        val color = if (gifticon.isUsed) Color.GRAY else Color.BLACK
        val secondaryColor = if (gifticon.isUsed) Color.GRAY else Color.RED

        holder.name.setTextColor(color)
        holder.expiration.setTextColor(secondaryColor)

        // 🔥 취소선 플래그 확실하게 ON/OFF
        if (gifticon.isUsed) {
            holder.name.paintFlags =
                holder.name.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.expiration.paintFlags =
                holder.expiration.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.name.paintFlags =
                holder.name.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.expiration.paintFlags =
                holder.expiration.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
    }


    override fun getItemCount() = gifticonList.size

    fun updateList(newList: List<Gifticon>) {
        gifticonList.clear()
        gifticonList.addAll(newList)
        notifyDataSetChanged()
    }
}

// ----------------------------------------------------------------------
// 👤 ProfileActivity
// ----------------------------------------------------------------------
class ProfileActivity : AppCompatActivity() {

    private val TAG = "ProfileActivity"

    private lateinit var tokenManager: TokenManager
    private lateinit var authRepository: AuthRepository

    private lateinit var tvHeaderTitle: TextView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var rvGifticons: RecyclerView
    private lateinit var tvEmptyState: TextView

    private lateinit var gifticonAdapter: GifticonAdapter
    private val gifticonList = mutableListOf<Gifticon>()

    // ----------------------------------------------------
    // ✅ 1. BroadcastReceiver 정의: OCR 완료 신호를 받으면 목록 갱신
    // ----------------------------------------------------
    private val listUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // ACTION_GIFTICON_LIST_UPDATED는 ImageObserverService에 정의된 상수라고 가정
            if (intent?.action == ImageObserverService.ACTION_GIFTICON_LIST_UPDATED) {
                Log.d(TAG, "🟢 OCR 완료 신호 수신됨. 기프티콘 목록 갱신 시작.")
                fetchAndBindGifticons() // 데이터 다시 불러오기
            }
        }
    }


    // 🔔 알림 권한 요청 런처
    private val requestNotificationPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Log.d(TAG, "🔔 알림 권한 허용됨")
            } else {
                Log.w(TAG, "🚫 알림 권한 거부됨")
                Toast.makeText(
                    this,
                    "알림을 받으려면 설정에서 알림 권한을 허용해 주세요.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        setContentView(R.layout.activity_profile)

        // 뷰 바인딩
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        tvUserName = findViewById(R.id.tvUserName)
        tvUserEmail = findViewById(R.id.tvUserEmail)
        bottomNav = findViewById(R.id.bottomNav)
        rvGifticons = findViewById(R.id.rvGifticons)
        tvEmptyState = findViewById(R.id.emptyState)

        // 리포지토리 준비
        tokenManager = TokenManager(applicationContext)
        authRepository = AuthRepository(tokenManager)

        // RecyclerView 설정
        gifticonAdapter = GifticonAdapter(gifticonList) { item ->
            updateGifticonStatus(item)
        }
        rvGifticons.apply {
            layoutManager = LinearLayoutManager(this@ProfileActivity)
            adapter = gifticonAdapter
            isNestedScrollingEnabled = false
        }

        // 상단 제목 고정
        tvHeaderTitle.text = "내 정보"

        // 인텐트 기본값 (최초 로그인 시 받은 값)
        tvUserName.text = intent.getStringExtra("userName") ?: "사용자 이름"
        tvUserEmail.text = intent.getStringExtra("userEmail") ?: "email@example.com"

        // 🌟 서비스 시작
        // (주의: ImageObserverService의 ACTION_GIFTICON_LIST_UPDATED 상수가 이 액티비티에서 접근 가능한지 확인해야 합니다.)
        startService(Intent(this, ImageObserverService::class.java))

        // ✅ 여기서 알림 권한 확인 및 요청
        askNotificationPermissionIfNeeded()

        // 화면 진입 시 사용자 정보 요청
        fetchAndBindUserProfile()

        // 하단 바 동작
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> {
                    Toast.makeText(this, "사용자 정보", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_map -> {
                    // 지도 화면으로 이동
                    val intent = Intent(this, MapsActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_add -> {
                    // OCR 화면으로 이동
                    val intent = Intent(this, OcrActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        bottomNav.selectedItemId = R.id.nav_profile
    }

    override fun onResume() {
        super.onResume()

        // 화면이 다시 포커스를 얻을 때마다 목록 갱신
        fetchAndBindGifticons()

        // ✅ 2. 리시버 등록 (onResume 시점에 등록)
        val filter = IntentFilter(ImageObserverService.ACTION_GIFTICON_LIST_UPDATED)
        LocalBroadcastManager.getInstance(this).registerReceiver(listUpdateReceiver, filter)
        Log.d(TAG, "LocalBroadcastReceiver 등록 완료.")
    }

    override fun onPause() {
        super.onPause()

        // ✅ 3. 리시버 해제 (onPause 시점에 해제, 메모리 누수 방지)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(listUpdateReceiver)
        Log.d(TAG, "LocalBroadcastReceiver 해제 완료.")
    }

    // 🔔 알림 권한 체크 & 요청
    private fun askNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun updateGifticonStatus(gifticon: Gifticon) {
        if (gifticon.isUsed) {
            // 사용 완료된 경우 -> 삭제 요청 (DELETE)
            Toast.makeText(this, "기프티콘 ${gifticon.id} 삭제 요청 중...", Toast.LENGTH_LONG).show()
            authRepository.deleteGifticon(gifticon.id) { isSuccess, responseBody ->
                runOnUiThread {
                    if (isSuccess) {
                        Toast.makeText(this, "✅ 기프티콘 삭제 성공! 목록을 갱신합니다.", Toast.LENGTH_SHORT).show()
                        fetchAndBindGifticons()
                    } else {
                        Log.e(TAG, "기프티콘 삭제 실패: $responseBody")
                        Toast.makeText(this, "삭제 처리 실패. 서버 응답을 확인하세요.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            // 미사용 상태인 경우 -> 사용 완료 요청 (PUT)
            Toast.makeText(this, "기프티콘 ${gifticon.id} 사용 처리 요청 중...", Toast.LENGTH_LONG).show()
            authRepository.updateGifticonUsedStatus(gifticon, true) { isSuccess, responseBody ->
                runOnUiThread {
                    if (isSuccess) {
                        Toast.makeText(this, "✅ 사용 처리 성공! 목록을 갱신합니다.", Toast.LENGTH_SHORT).show()
                        fetchAndBindGifticons()
                    } else {
                        Log.e(TAG, "기프티콘 상태 변경 실패: $responseBody")
                        Toast.makeText(this, "사용 처리 실패. 서버 응답을 확인하세요.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun fetchAndBindUserProfile() {
        authRepository.fetchUserProfile { isSuccess, responseBody ->
            runOnUiThread {
                if (!isSuccess || responseBody.isNullOrBlank()) {
                    Log.w(TAG, "프로필 요청 실패 또는 빈 응답")
                    return@runOnUiThread
                }
                try {
                    val json = JSONObject(responseBody)
                    val name = when {
                        json.has("name") -> json.optString("name")
                        json.has("username") -> json.optString("username")
                        json.has("nickname") -> json.optString("nickname")
                        else -> tvUserName.text.toString()
                    }
                    val email = when {
                        json.has("email") -> json.optString("email")
                        json.has("mail") -> json.optString("mail")
                        else -> tvUserEmail.text.toString()
                    }
                    tvUserName.text = name
                    tvUserEmail.text = email
                } catch (e: Exception) {
                    Log.e(TAG, "프로필 파싱 에러", e)
                }
            }
        }
    }

    private fun fetchAndBindGifticons() {
        authRepository.fetchGifticonList { isSuccess, responseBody ->
            runOnUiThread {
                if (!isSuccess || responseBody.isNullOrBlank()) {
                    Log.w(TAG, "기프티콘 목록 요청 실패 또는 빈 응답")
                    tvEmptyState.visibility = View.VISIBLE
                    rvGifticons.visibility = View.GONE
                    return@runOnUiThread
                }
                try {
                    val jsonArray = JSONArray(responseBody)
                    val newGifticonList = mutableListOf<Gifticon>()

                    for (i in 0 until jsonArray.length()) {
                        val gifticonJson = jsonArray.getJSONObject(i)
                        newGifticonList.add(
                            Gifticon(
                                id = gifticonJson.optString("gifticonId", ""),
                                name = gifticonJson.optString("storeName", "제목 없음"),
                                expiration = gifticonJson.optString("expirationDate", "만료일 미정"),
                                isUsed = gifticonJson.optBoolean("isUsed", false)
                            )
                        )
                    }

                    gifticonAdapter.updateList(newGifticonList)

                    if (newGifticonList.isEmpty()) {
                        tvEmptyState.visibility = View.VISIBLE
                        rvGifticons.visibility = View.GONE
                    } else {
                        tvEmptyState.visibility = View.GONE
                        rvGifticons.visibility = View.VISIBLE
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "기프티콘 목록 파싱 에러", e)
                    tvEmptyState.visibility = View.VISIBLE
                    rvGifticons.visibility = View.GONE
                }
            }
        }
    }
}