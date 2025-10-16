package com.example.giftguard.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.giftguard.layout.MainLayout
import com.example.giftguard.screens.AddGifticonScreen
import com.example.giftguard.screens.DashboardScreen
import com.example.giftguard.screens.DetailScreen
import com.example.giftguard.screens.GeofenceAlertScreen
import com.example.giftguard.screens.StoreMapScreen
import com.example.giftguard.viewmodels.GiftconViewModel

/**
 * 전체 앱의 내비게이션 호스트를 설정하는 최상위 컴포저블입니다.
 * 모든 화면 경로를 정의하고 해당 화면 컴포저블을 연결합니다.
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    viewModel: GiftconViewModel,
    modifier: Modifier = Modifier
) {
    // NavHost를 사용하여 내비게이션 그래프를 정의합니다.
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        // --------------------------------------------------
        // 1. Dashboard (메인 화면)
        // --------------------------------------------------
        composable(Screen.Dashboard.route) {
            MainLayout(navController = navController) { padding ->
                DashboardScreen(
                    navController = navController,
                    paddingValues = padding,
                    viewModel = viewModel
                )
            }
        }

        // --------------------------------------------------
        // 2. AddGifticon (등록/수정 화면) - 🌟 경로 통합 및 크래시 방지 🌟
        // --------------------------------------------------
        // NavHost에 'routeWithArgs' 하나만 등록합니다. (예: "add_gifticon/{giftconId}")
        composable(
            route = Screen.AddGifticon.routeWithArgs,
            arguments = listOf(
                navArgument("giftconId") {
                    type = NavType.StringType
                    // 🚨 핵심 수정: 등록 모드에서 인자를 생략하고 호출할 수 있도록 defaultValue를 null로 설정합니다.
                    // 이 설정 덕분에, 'add_gifticon'으로만 호출해도 이 블록으로 연결됩니다.
                    defaultValue = null
                    nullable = true
                }
            )
        ) { backStackEntry ->
            // 인자 추출: 인자가 없으면 (등록 모드) null이 됩니다.
            val giftconId = backStackEntry.arguments?.getString("giftconId")

            MainLayout(navController = navController) { padding ->
                AddGifticonScreen(
                    navController = navController,
                    paddingValues = padding,
                    viewModel = viewModel,
                    giftconId = giftconId // 등록 모드: null, 수정 모드: ID
                )
            }
        }

        // 🚨 중요: 인자가 없는 경로 (Screen.AddGifticon.createRoute())에 대한
        // composable 블록은 위의 통합된 블록에서 처리되므로 제거했습니다.
        // 기존 코드에서 크래시를 유발했던 블록이었습니다.

        // --------------------------------------------------
        // 3. StoreMap (매장 찾기 화면)
        // --------------------------------------------------
        composable(Screen.StoreMap.route) {
            MainLayout(navController = navController) { padding ->
                StoreMapScreen(
                    navController = navController,
                    paddingValues = padding,
                    viewModel = viewModel
                )
            }
        }

        // --------------------------------------------------
        // 4. GeofenceAlert (위치 알림 화면)
        // --------------------------------------------------
        composable(Screen.GeofenceAlert.route) {
            MainLayout(navController = navController) { padding ->
                GeofenceAlertScreen(
                    navController = navController,
                    paddingValues = padding,
                    viewModel = viewModel
                )
            }
        }

        // --------------------------------------------------
        // 5. Detail (상세 화면)
        // --------------------------------------------------
        composable(
            route = Screen.Detail.route, // "gifticon_detail/{giftconId}"
            arguments = listOf(
                navArgument(name = "giftconId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val idFromArgs = backStackEntry.arguments?.getString("giftconId") ?: "unknown"
            MainLayout(navController = navController) { padding ->
                DetailScreen(
                    navController = navController,
                    giftconId = idFromArgs,
                    paddingValues = padding,
                    viewModel = viewModel
                )
            }
        }
    }
}
