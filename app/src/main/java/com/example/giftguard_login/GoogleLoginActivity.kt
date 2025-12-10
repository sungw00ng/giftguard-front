package com.example.giftguard_login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.giftguard_login.databinding.ActivityGoogleLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging

class GoogleLoginActivity : AppCompatActivity() {

    private val TAG = "GoogleLoginActivity"
    private val RC_SIGN_IN = 9001

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var binding: ActivityGoogleLoginBinding

    private lateinit var tokenManager: TokenManager
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        // ⭐ 추가: 앱 시작 시 Places SDK 초기화 (한 번만)
        PlacesUtil.ensureInitialized(this)

        binding = ActivityGoogleLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "✅ GoogleLoginActivity started")

        // TokenManager / AuthRepository 초기화
        tokenManager = TokenManager(applicationContext)
        authRepository = AuthRepository(tokenManager)

        // GoogleSignInClient 설정
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_login_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // 로그인 버튼
        binding.googleSignInButtonCustom.setOnClickListener {
            Log.d(TAG, "✅ Google Sign-In button clicked")
            signIn()
        }

        // ❌ "내 정보 불러오기" 버튼 관련 코드가 제거되었습니다.
        /*
        binding.buttonFetchProfile.setOnClickListener {
            Log.d(TAG, "➡️ 사용자정보 화면으로 이동 버튼 클릭")
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        */
    }

    private fun signIn() {
        Log.d(TAG, "🟩 Starting Google Sign-In flow...")
        val signInIntent: Intent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    @Deprecated("onActivityResult is deprecated. Consider ActivityResult APIs later.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            Log.d(TAG, "🟦 onActivityResult called (RC_SIGN-IN)")
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            Log.d(TAG, "🟨 handleSignInResult() 실행됨")
            val account = completedTask.getResult(ApiException::class.java)
            val idToken = account.idToken

            if (idToken != null) {
                Log.d(TAG, "✅ Google Sign-In 성공! ID Token으로 JWT 교환 시작...")

                // 화면 전환 시 전달할 사용자 정보
                val displayName = account.displayName
                val email = account.email

                // 서버에 ID 토큰 교환 요청
                authRepository.exchangeIdToken(idToken) { isSuccess ->
                    runOnUiThread {
                        if (isSuccess) {
                            Log.d(TAG, "✅ JWT 교환 성공. 이제 FCM 토큰 서버로 전송 시도")

                            // 1) 먼저 FCM 토큰 가져오기
                            FirebaseMessaging.getInstance().token
                                .addOnSuccessListener { fcmToken ->
                                    Log.d(TAG, "📨 FCM 토큰 획득: $fcmToken")

                                    // 2) JWT는 방금 exchangeIdToken 안에서 저장됐으므로,
                                    //    바로 서버에 FCM 토큰 전송
                                    authRepository.sendFcmTokenToServer(fcmToken) { ok, msg ->
                                        runOnUiThread {
                                            if (ok) {
                                                Log.d(TAG, "✅ FCM 토큰 등록 성공")
                                            } else {
                                                Log.e(TAG, "❌ FCM 토큰 등록 실패: $msg")
                                            }

                                            // 3) FCM 성공/실패와 관계 없이 로그인 자체는 성공이니 화면 이동
                                            Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()
                                            goToProfile(displayName, email)
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "❌ FCM 토큰 가져오기 실패: ${e.message}", e)

                                    // FCM 토큰 못 보내더라도, 로그인은 성공했으니 그냥 넘어가도 됨
                                    Toast.makeText(
                                        this,
                                        "로그인 성공! (FCM 토큰 전송 실패)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    goToProfile(displayName, email)
                                }

                        } else {
                            Toast.makeText(this, "로그인 실패: JWT 교환 오류.", Toast.LENGTH_LONG).show()
                        }
                    }
                }

            } else {
                Log.e(TAG, "❌ idToken is null. Check Web Client ID or requestIdToken() setup.")
                Toast.makeText(this, "로그인 실패(토큰 없음). 설정을 확인하세요.", Toast.LENGTH_SHORT).show()
            }

        } catch (e: ApiException) {
            Log.w(TAG, "Google sign in failed", e)
            runOnUiThread {
                Toast.makeText(this, "구글 로그인 실패: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 프로필 화면으로 이동 (레이아웃만 있어도 OK)
    private fun goToProfile(userName: String?, userEmail: String?) {
        val intent = Intent(this, ProfileActivity::class.java).apply {
            putExtra("userName", userName ?: "사용자 이름")
            putExtra("userEmail", userEmail ?: "email@example.com")
        }
        startActivity(intent)
        finish() // 로그인 화면 종료 (원치 않으면 제거)
    }
}
