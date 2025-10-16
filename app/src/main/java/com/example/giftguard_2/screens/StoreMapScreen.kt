package com.example.giftguard.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.giftguard.viewmodels.GiftconViewModel // ViewModel import 추가

/**
 * 매장 찾기 화면입니다. (지도 Placeholder)
 */
@Composable
fun StoreMapScreen(
    navController: NavController,
    paddingValues: PaddingValues,
    viewModel: GiftconViewModel // 👈 ViewModel 인자 추가
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "지도 아이콘",
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "주변 매장 지도 검색 (준비 중)",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "기프티콘을 사용할 수 있는 주변 매장들을 지도에서 찾는 기능이 여기에 구현될 예정입니다. (현재 ViewModel 상태 사용 가능)",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(0.8f)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // TODO: 지도 UI (Google Maps Compose) 및 검색 필터링 기능 구현

        Button(
            onClick = {
                navController.popBackStack() // 이전 화면으로 돌아가기
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("대시보드로 돌아가기")
        }
    }
}
