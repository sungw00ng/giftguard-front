package com.example.giftguard.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.* // remember, getValue, setValue, LaunchedEffect, collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.giftguard.viewmodels.GiftconViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


/**
 * 기프티콘 등록/수정 화면 Wrapper Composable입니다.
 * @param giftconId 수정 모드일 때 기프티콘의 ID를 받습니다. "null" 문자열이거나 null이면 등록 모드입니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGifticonScreen(
    navController: NavController,
    paddingValues: PaddingValues,
    viewModel: GiftconViewModel,
    giftconId: String? = null // AppNavigation에서 전달받은 ID (null이면 등록 모드)
) {
    // 🌟 엄격한 모드 판단 🌟
    val isEditMode = !giftconId.isNullOrBlank() && giftconId != "null"

    if (isEditMode) {
        // 수정 모드
        GifticonEditingContent(
            navController = navController,
            // 🌟 TopAppBar가 추가되면서 paddingValues는 상위 Scaffold가 아닌 내부에서 처리하도록 변경하거나,
            // 이 Wrapper에서 전달된 패딩을 무시하고 내부 Scaffold에 맡기는 것이 일반적입니다.
            // 여기서는 내부 Scaffold에서 처리할 수 있도록 paddingValues 전달을 제거합니다.
            paddingValues = PaddingValues(0.dp), // 이 파일을 수정했으므로, 0dp로 재정의
            viewModel = viewModel,
            giftconId = giftconId!!
        )
    } else {
        // 등록 모드
        GifticonRegistrationContent(
            navController = navController,
            paddingValues = PaddingValues(0.dp), // 이 파일을 수정했으므로, 0dp로 재정의
            viewModel = viewModel
        )
    }
}


/**
 * 기프티콘 등록 모드 전용 Composable입니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GifticonRegistrationContent(
    navController: NavController,
    paddingValues: PaddingValues,
    viewModel: GiftconViewModel
) {
    val formState by viewModel.formState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 🌟 등록 모드 진입 시 폼 초기화 강제 (데이터 잔존 방지)
    LaunchedEffect(Unit) {
        viewModel.clearSaveError()
        viewModel.resetFormState()
    }

    // 🌟🌟🌟 제목 설정: 등록 모드 🌟🌟🌟
    val titleText = "기프티콘 등록"
    val buttonText = "기프티콘 등록하기"

    // 공통 로직 호출
    GiftconFormContent(
        navController = navController,
        paddingValues = paddingValues,
        viewModel = viewModel,
        formState = formState,
        snackbarHostState = snackbarHostState,
        titleText = titleText, // "기프티콘 등록" 전달
        buttonText = buttonText
    )
}


/**
 * 기프티콘 수정 모드 전용 Composable입니다.
 * 수정 데이터를 로드하고 로딩 상태를 처리합니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GifticonEditingContent(
    navController: NavController,
    paddingValues: PaddingValues,
    viewModel: GiftconViewModel,
    giftconId: String
) {
    val formState by viewModel.formState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 🌟 수정 모드 진입 시 데이터 로드 및 오류 클리어 강제
    LaunchedEffect(giftconId) {
        viewModel.clearSaveError()
        // ID 변경 시에만 기존 데이터 로드 시도
        // NOTE: ViewModel의 startEditing 함수는 데이터 로드 중 isSaving을 true로 설정해야 합니다.
        viewModel.startEditing(giftconId)
    }

    // 🌟🌟🌟 제목 설정: 수정 모드 🌟🌟🌟
    val titleText = "기프티콘 수정"
    val buttonText = "기프티콘 수정 완료" // 🌟 버튼 텍스트 수정 완료 🌟

    // 🌟 수정: 데이터 로드 중에는 로딩 인디케이터를 표시하여 UI 깜빡임 및 잔존 상태 문제를 방지합니다.
    if (formState.isSaving) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        // 공통 로직 호출 (데이터 로드 완료 후)
        GiftconFormContent(
            navController = navController,
            paddingValues = paddingValues,
            viewModel = viewModel,
            formState = formState,
            snackbarHostState = snackbarHostState,
            titleText = titleText, // "기프티콘 수정" 전달
            buttonText = buttonText
        )
    }
}


/**
 * 등록/수정 모드에 관계없이 폼을 렌더링하는 공통 Composable입니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GiftconFormContent(
    navController: NavController,
    paddingValues: PaddingValues, // 현재 사용되지 않음 (Scaffold에서 자체적으로 처리)
    viewModel: GiftconViewModel,
    formState: com.example.giftguard.viewmodels.GiftconFormState, // 전체 formState 타입을 명시
    snackbarHostState: SnackbarHostState,
    titleText: String,
    buttonText: String
) {
    // DatePicker 관련 상태.
    var showDatePicker by remember { mutableStateOf(false) }

    // 🌟 DatePickerState를 formState.expiryDate에 반응하도록 key를 설정하여 명시적으로 리멤버합니다.
    val datePickerState = remember(formState.expiryDate) {
        val initialTime = formState.expiryDate.time
        // 날짜가 너무 오래된(Date(0)) 경우가 아니라면 해당 날짜를 사용
        val initialMillis = if (initialTime > 86400000L) initialTime else System.currentTimeMillis()

        DatePickerState(
            initialSelectedDateMillis = initialMillis,
            yearRange = (2020..2050),
            initialDisplayMode = DisplayMode.Picker,
            initialDisplayedMonthMillis = null
        )
    }

    // ViewModel에서 전달된 successMessage 사용
    LaunchedEffect(formState.saveSuccess, formState.successMessage) {
        // saveSuccess가 true이고, ViewModel에서 설정한 메시지가 있을 때만 Snackbar를 표시합니다.
        if (formState.saveSuccess && formState.successMessage != null) {
            snackbarHostState.showSnackbar(
                message = formState.successMessage, // ViewModel에서 분기된 정확한 메시지 사용
                withDismissAction = true
            )
            viewModel.resetFormState()
            navController.popBackStack()
        }
    }

    // Snackbar Leak 방지 수정 부분
    LaunchedEffect(formState.saveError) {
        formState.saveError?.let { error ->
            snackbarHostState.showSnackbar(
                message = "오류: $error",
                withDismissAction = true
            )
            // 🌟 중요: 스낵바를 띄운 직후, ViewModel에 오류 상태를 즉시 초기화하도록 요청합니다.
            viewModel.clearSaveError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // 🌟🌟🌟 TopAppBar 추가 및 제목 동적 설정 🌟🌟🌟
        topBar = {
            TopAppBar(
                title = { Text(titleText) }, // 🌟 등록/수정 모드 제목 사용 (왼쪽 상단)
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "뒤로 가기"
                        )
                    }
                }
            )
        },
        content = { scaffoldPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // 🌟 TopAppBar의 패딩을 포함한 Scaffold 패딩 적용
                    .padding(scaffoldPadding)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // 🌟🌟🌟 중앙 제목 다시 표시 (복구) 🌟🌟🌟
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                // 🌟🌟🌟 복구 완료 🌟🌟🌟
                Spacer(modifier = Modifier.height(24.dp))

                // 매장명 (브랜드) 입력
                OutlinedTextField(
                    value = formState.brand,
                    onValueChange = viewModel::updateBrand,
                    label = { Text("매장명 (브랜드)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                // 상품명 입력
                OutlinedTextField(
                    value = formState.productName,
                    onValueChange = viewModel::updateProductName,
                    label = { Text("상품명") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                // 유효기간 입력 (DatePicker 연동)
                val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())
                OutlinedTextField(
                    value = dateFormat.format(formState.expiryDate),
                    onValueChange = { /* 텍스트 수기 입력은 막습니다. */ },
                    label = { Text("유효기간") },
                    readOnly = true, // 수기 입력 방지
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Filled.CalendarToday, contentDescription = "날짜 선택")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true } // 필드 클릭 시에도 다이얼로그 열기
                )
                Spacer(modifier = Modifier.height(24.dp))

                // TODO: 여기에 바코드 번호, 금액, 카테고리 등 다른 폼 요소들이 들어갈 수 있습니다.

                Spacer(modifier = Modifier.height(48.dp))

                // 등록/수정 버튼
                Button(
                    onClick = {
                        viewModel.saveGiftcon() // 저장 로직 (ViewModel 내에서 등록/수정 분기 처리)
                    },
                    enabled = !formState.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    if (formState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        // 🌟 분리된 컴포넌트에서 전달된 동적 버튼 텍스트 🌟
                        Text(buttonText)
                    }
                }
            }
        }
    )

    // DatePickerDialog 표시
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            // 선택된 밀리초를 Date 객체로 변환하여 ViewModel에 반영
                            viewModel.updateExpiryDate(Date(millis))
                        }
                        showDatePicker = false
                    },
                    enabled = datePickerState.selectedDateMillis != null
                ) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                    }
                ) {
                    Text("취소")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
