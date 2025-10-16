package com.example.giftguard.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
// 🌟 AutoMirrored 오류 해결을 위해 표준 ArrowBack 아이콘 사용으로 변경했습니다.
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.giftguard.navigation.Screen

/**
 * 앱의 주요 레이아웃 구조(Scaffold, Top Bar, Floating Action Button 등)를 정의합니다.
 * 모든 주요 화면은 이 레이아웃 컴포저블 내부에 배치됩니다.
 *
 * @param navController NavController 인스턴스
 * @param content 화면별 콘텐츠를 그리는 람다 함수
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(
    navController: NavController,
    content: @Composable (paddingValues: PaddingValues) -> Unit
) {
    // 현재 destination의 route (예: "dashboard" 또는 "add_gifticon/{giftconId}")
    val currentRoute = navController.currentBackStackEntry?.destination?.route

    // 현재 back stack entry를 가져와서 인자 접근에 사용합니다.
    val backStackEntry = navController.currentBackStackEntry

    // Top Bar의 제목을 결정하는 함수
    val topBarTitle = @Composable {
        val titleText = when (currentRoute) {
            Screen.Dashboard.route -> "기프티콘 보관함"

            // 🌟🌟🌟 AddGifticon 경로에 대한 제목 동적 분기 처리 🌟🌟🌟
            Screen.AddGifticon.routeWithArgs -> { // "add_gifticon/{giftconId}"와 정확히 매칭되는 경우
                // NavHost에 등록된 경로와 일치하므로 인자를 확인합니다.
                val giftconId = backStackEntry?.arguments?.getString("giftconId")

                // ID가 전달되었고 ("null"이 아님) 유효하면 수정 모드입니다.
                val isEditing = !giftconId.isNullOrBlank() && giftconId != "null"

                if (isEditing) {
                    "기프티콘 수정" // ID가 전달되면 수정 모드
                } else {
                    "기프티콘 등록" // ID가 없거나 "null"이면 등록 모드
                }
            }
            // 🌟 Detail 화면도 인자가 필요하므로 routeWithArgs를 사용해야 합니다. 🌟
            Screen.Detail.route -> "기프티콘 상세"

            Screen.StoreMap.route -> "매장 찾기"
            Screen.GeofenceAlert.route -> "위치 알림 설정"

            else -> "GiftGuard"
        }
        Text(titleText)
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = topBarTitle,
                // Dashboard가 아닌 화면에서는 뒤로가기 버튼을 표시
                navigationIcon = {
                    if (currentRoute != Screen.Dashboard.route) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로 가기")
                        }
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    // NOTE: AppThemeColors가 정의되지 않았을 가능성을 고려하여 기본 색상 사용
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            // 메인 화면(Dashboard)일 때만 FAB 표시
            if (currentRoute == Screen.Dashboard.route) {
                FloatingActionButton(
                    onClick = {
                        // 등록 모드 경로 사용
                        navController.navigate(Screen.AddGifticon.createRoute())
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Filled.Add, "새 기프티콘 등록")
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        content(paddingValues)
    }
}
