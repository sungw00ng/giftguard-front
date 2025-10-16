package com.example.giftguard.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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
 * 위치 기반 알림 설정 화면입니다. (Geofence Placeholder)
 */
@Composable
fun GeofenceAlertScreen(
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
            imageVector = Icons.Default.Warning,
            contentDescription = "알림 아이콘",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "위치 기반 알림 설정 (미구현)",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "특정 매장 근처에 도착했을 때 기프티콘 사용 알림을 설정하는 기능이 여기에 구현될 예정입니다. (ViewModel 상태 사용 가능)",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(0.8f)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // TODO: 위치 권한 요청 및 지오펜스 설정 UI 구현

        Button(
            onClick = {
                navController.popBackStack() // 이전 화면으로 돌아가기
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("나중에 설정하기")
        }
    }
}