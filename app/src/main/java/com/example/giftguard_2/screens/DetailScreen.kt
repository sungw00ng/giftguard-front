package com.example.giftguard.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons // 👈 아이콘 Import
import androidx.compose.material.icons.filled.Image // 👈 아이콘 Import
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.giftguard.R // R 파일이 실제 프로젝트 경로에 맞게 정의되었다고 가정
import com.example.giftguard.layout.AppThemeColors
import com.example.giftguard.viewmodels.GiftconViewModel // ViewModel import 추가
import com.example.giftguard.navigation.Screen // 🌟 Screen import 추가 🌟
import java.text.SimpleDateFormat // 👈 SimpleDateFormat import 추가
import java.util.Locale // 👈 Locale import 추가

/**
 * 기프티콘 상세 정보를 표시하는 화면입니다.
 */
@Composable
fun DetailScreen(
    navController: NavController,
    giftconId: String,
    paddingValues: PaddingValues,
    viewModel: GiftconViewModel // 👈 ViewModel 인자 추가
) {
    // 🌟 수정: StateFlow의 상태를 Compose에서 안전하게 관찰하도록 collectAsState() 사용
    val uiState by viewModel.uiState.collectAsState()

    // 임시 더미 데이터 (실제 데이터로 대체 필요)
    val dummyData = uiState.giftconList.find { it.id == giftconId }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp)
    ) {
        if (dummyData == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "기프티콘 ID '$giftconId'를 찾을 수 없습니다.",
                    color = MaterialTheme.colorScheme.error
                )
            }
            return
        }

        // 1. 상품 이미지 및 바코드 영역
        Spacer(modifier = Modifier.height(20.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // 🌟 수정: R.drawable.ic_placeholder 대신 Icon 컴포저블을 사용하여 임시 대체
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Image,
                        contentDescription = "상품 이미지 Placeholder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 바코드 이미지 Placeholder
                Text(
                    text = "바코드 이미지 Placeholder",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 2. 상세 정보 영역
        Text(
            text = dummyData.brand,
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = AppThemeColors.PrimaryDark
        )
        Text(
            text = dummyData.productName,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Divider(color = Color(0xFFE0E0E0))

        Spacer(modifier = Modifier.height(16.dp))

        // 유효기간
        DetailRow(
            label = "유효기간",
            value = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(dummyData.expiryDate),
            highlight = true
        )
        // 사용 여부
        DetailRow(
            label = "사용 여부",
            value = if (dummyData.isUsed) "사용 완료" else "사용 가능",
            valueColor = if (dummyData.isUsed) Color.Red else Color.Green
        )
        // 등록일
        DetailRow(
            label = "등록일",
            value = "2024.10.15" // 더미 데이터
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 3. 버튼 영역 (사용 처리, 수정)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    // 🌟 최종 수정: Screen.kt의 createRoute를 사용하여 ID를 전달 🌟
                    navController.navigate(Screen.AddGifticon.createRoute(dummyData.id))
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("수정하기")
            }

            Button(
                onClick = {
                    // TODO: 사용 처리 로직 구현 (viewModel.markAsUsed(giftconId))
                    // 현재는 더미 데이터이므로 실제 작동하지 않습니다.
                },
                modifier = Modifier.weight(1f),
                enabled = !dummyData.isUsed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (dummyData.isUsed) Color(0xFFAAAAAA) else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (dummyData.isUsed) "사용 완료됨" else "사용 처리")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun DetailRow(label: String, value: String, highlight: Boolean = false, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal),
            color = valueColor
        )
    }
}