package com.example.giftguard.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.giftguard.layout.AppThemeColors
import com.example.giftguard.navigation.Screen
import com.example.giftguard.viewmodels.GiftconViewModel
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 기프티콘 목록을 보여주는 메인 대시보드 화면입니다.
 * FAB를 포함하여 기프티콘 등록 화면으로 이동 기능을 제공합니다.
 */
@Composable
fun DashboardScreen(
    navController: NavController,
    paddingValues: PaddingValues,
    viewModel: GiftconViewModel // ViewModel 인자를 받습니다.
) {
    // ViewModel의 상태를 관찰합니다.
    val uiState by viewModel.uiState.collectAsState()
    val giftcons = uiState.giftconList

    // 🌟 Scaffold를 사용하여 FAB를 포함시킵니다.
    Scaffold(
        // MainLayout에서 받은 패딩을 여기서는 적용하지 않고, LazyColumn에서 사용합니다.
        // Scaffold는 TopBar 공간을 이미 고려하므로 paddingValues를 여기에 직접 넣지 않습니다.
        modifier = Modifier.padding(paddingValues),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // 🌟 FAB 클릭 시, 인자 없는 등록 경로로 이동
                    navController.navigate(Screen.AddGifticon.createRoute())
                }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "기프티콘 등록")
            }
        },
        content = { scaffoldPadding ->
            // Scaffold가 제공하는 내부 패딩(TopBar 높이 등)을 적용합니다.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding)
            ) {
                // UI 제목 (스크린샷에 맞춰 제목 영역을 Column에 포함)
                Text(
                    text = "기프티콘 보관함",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )

                // 로딩 상태 처리
                if (uiState.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                if (giftcons.isEmpty() && !uiState.isLoading) {
                    // 기프티콘이 없을 경우 메시지 표시
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "등록된 기프티콘이 없습니다.\n플러스 버튼을 눌러 추가하세요.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // 실제 기프티콘 목록 표시 (LazyColumn)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(giftcons, key = { it.id }) { giftcon ->
                            GiftconItem(
                                brand = giftcon.brand,
                                productName = giftcon.productName,
                                expiryDate = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(giftcon.expiryDate),
                                isUsed = giftcon.isUsed,
                                onClick = {
                                    // 항목 클릭 시 상세 화면으로 이동
                                    navController.navigate(Screen.Detail.createRoute(giftcon.id))
                                }
                            )
                        }
                    }
                }
            }
        }
    )
}

/**
 * 기프티콘 목록 아이템 컴포저블
 */
@Composable
fun GiftconItem(
    brand: String,
    productName: String,
    expiryDate: String,
    isUsed: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick) // 클릭 리스너 적용
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = brand,
                    style = MaterialTheme.typography.titleLarge,
                    color = AppThemeColors.PrimaryDark
                )
                Text(
                    text = productName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 유효기간/사용 상태
            Text(
                // D-day 계산 로직이 없으므로 임시로 D-day: 만료일 표시
                text = if (isUsed) "사용 완료" else "D-day: ${expiryDate}",
                style = MaterialTheme.typography.labelLarge,
                color = if (isUsed) AppThemeColors.Gray600 else AppThemeColors.Accent,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
