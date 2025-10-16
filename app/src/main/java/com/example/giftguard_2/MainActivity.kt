package com.example.giftguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.giftguard.data.AppContainer
import com.example.giftguard.data.DefaultAppContainer
import com.example.giftguard.layout.GiftGuardTheme
import com.example.giftguard.navigation.AppNavigation
import com.example.giftguard.viewmodels.GiftconViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * 앱의 메인 진입점인 Activity입니다.
 * Compose 환경을 설정하고, 의존성 컨테이너를 초기화하며, 최상위 내비게이션을 호스팅합니다.
 */
class MainActivity : ComponentActivity() {

    // 🌟 1. 앱 컨테이너 인스턴스 (의존성 관리)
    private lateinit var container: AppContainer

    // 🌟 2. GiftconViewModel 인스턴스 생성 및 주입
    // AppContainer에서 제공하는 Factory를 사용하여 ViewModel을 생성합니다.
    private val giftconViewModel: GiftconViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(GiftconViewModel::class.java)) {
                    // DefaultAppContainer에서 미리 생성해 둔 싱글톤 ViewModel을 반환
                    @Suppress("UNCHECKED_CAST")
                    return container.giftconViewModel as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🌟 1. 의존성 컨테이너 초기화
        container = DefaultAppContainer()

        // 이 시점에서 giftconViewModel이 초기화되며, init 블록의 loadGiftcons()가 실행됩니다.

        setContent {
            GiftGuardTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 🌟 수정: ViewModel 인스턴스를 Composable 함수에 전달합니다.
                    GiftGuardApp(viewModel = giftconViewModel)
                }
            }
        }
    }
}

/**
 * 앱의 최상위 컴포저블입니다.
 * 내비게이션 컨트롤러를 생성하고 AppNavigation을 호스팅합니다.
 * @param viewModel ViewModel 인스턴스 (Activity에서 주입됨)
 */
@Composable
fun GiftGuardApp(viewModel: GiftconViewModel) { // 👈 ViewModel 인자 받도록 수정
    // NavController 인스턴스를 생성하고 상태를 관리합니다.
    val navController = rememberNavController()

    // 🌟 수정: AppNavigation에 ViewModel 인스턴스를 전달합니다.
    AppNavigation(navController = navController, viewModel = viewModel)
}
