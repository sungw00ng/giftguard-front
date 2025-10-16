package com.example.giftguard.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giftguard.data.GiftconRepository
import com.example.giftguard.entities.Giftcon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

// --------------------------------------------------
// 🌟 폼 상태를 나타내는 데이터 클래스 (imageUrl 사용)
// --------------------------------------------------
data class GiftconFormState(
    val brand: String = "",
    val productName: String = "",
    val expiryDate: Date = Date(System.currentTimeMillis() + 86400000L * 30), // 기본값: 30일 후
    val price: String = "", // 입력 편의를 위해 String으로 관리 (선택 사항)
    val imageUrl: String? = null, // 이미지 URL
    val barcodeNumber: String = "", // 바코드 번호 (선택 사항)
    val memo: String = "", // 메모 (선택 사항)
    val category: String = "기타", // 카테고리 (선택 사항)
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val saveError: String? = null,
    val successMessage: String? = null // 🌟🌟🌟 추가: 성공 시 표시할 메시지
)

/**
 * UI 상태를 나타내는 데이터 클래스입니다.
 */
data class GiftconUiState(
    val giftconList: List<Giftcon> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * 기프티콘 데이터를 관리하고 UI에 노출하는 ViewModel입니다.
 *
 * @param giftconRepository 데이터 접근을 담당하는 Repository
 */
class GiftconViewModel(
    private val giftconRepository: GiftconRepository // Repository를 주입받습니다.
) : ViewModel() {

    // 🌟 대시보드 UI 상태
    private val _uiState = MutableStateFlow(GiftconUiState(isLoading = true))
    val uiState: StateFlow<GiftconUiState> = _uiState.asStateFlow()

    // 🌟 기프티콘 추가/수정 폼 상태
    private val _formState = MutableStateFlow(GiftconFormState())
    val formState: StateFlow<GiftconFormState> = _formState.asStateFlow()

    // 🌟 현재 수정 중인 기프티콘의 ID를 저장할 StateFlow 추가
    private val _currentGiftconId = MutableStateFlow<String?>(null)

    init {
        loadGiftcons()
    }

    /**
     * Repository를 통해 기프티콘 목록을 로드하고 상태를 업데이트합니다.
     * (TODO: 실제 Flow 연결 시 이 함수는 초기 로드 외에는 필요 없을 수 있습니다.)
     */
    fun loadGiftcons() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                // TODO: 실제 Repository의 Flow를 연결하여 데이터 변경 시 자동 업데이트되도록 구현 필요
                // 현재는 임시 더미 데이터를 사용
                val dummyList = createDummyGiftcons()
                _uiState.value = _uiState.value.copy(
                    giftconList = dummyList,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "데이터 로드 실패: ${e.localizedMessage}"
                )
            }
        }
    }

    // --------------------------------------------------
    // 🌟 폼 상태 업데이트 함수
    // --------------------------------------------------

    fun updateBrand(brand: String) {
        _formState.update { it.copy(brand = brand) }
    }

    fun updateProductName(productName: String) {
        _formState.update { it.copy(productName = productName) }
    }

    fun updateExpiryDate(date: Date) {
        _formState.update { it.copy(expiryDate = date) }
    }

    fun updatePrice(price: String) {
        _formState.update { it.copy(price = price) }
    }

    fun updateBarcodeNumber(number: String) {
        _formState.update { it.copy(barcodeNumber = number) }
    }

    fun updateMemo(memo: String) {
        _formState.update { it.copy(memo = memo) }
    }

    fun updateCategory(category: String) {
        _formState.update { it.copy(category = category) }
    }

    // --------------------------------------------------
    // 🌟 오류 상태 초기화 함수 (AddGifticonScreen에서 호출됨)
    // --------------------------------------------------

    /**
     * 저장 성공/오류 메시지 상태를 초기화합니다.
     */
    fun clearSaveError() {
        _formState.update { currentState ->
            currentState.copy(
                saveError = null,
                saveSuccess = false,
                successMessage = null // 🌟🌟🌟 성공 메시지도 초기화
            )
        }
    }

    // --------------------------------------------------
    // 🌟 수정 시작 함수
    // --------------------------------------------------

    /**
     * 특정 기프티콘 ID를 기반으로 폼 상태를 초기화합니다.
     * 수정 화면 진입 시 사용됩니다.
     */
    fun startEditing(giftconId: String) {
        // ID 저장
        _currentGiftconId.value = giftconId

        // 🌟 현재는 임시 목록에서 찾지만, 실제는 Repository에서 단일 항목을 가져와야 합니다.
        val giftconToEdit = _uiState.value.giftconList.find { it.id == giftconId }

        giftconToEdit?.let { giftcon ->
            _formState.value = GiftconFormState(
                brand = giftcon.brand,
                productName = giftcon.productName,
                expiryDate = giftcon.expiryDate,
                price = if (giftcon.price > 0.0) giftcon.price.toString() else "",
                imageUrl = giftcon.imageUrl,
                barcodeNumber = giftcon.barcodeNumber,
                memo = giftcon.memo,
                category = giftcon.category
            )
        } ?: run {
            // 해당 ID의 기프티콘이 없을 경우, 폼 상태를 리셋하여 등록 모드로 전환
            resetFormState()
        }
    }

    // --------------------------------------------------
    // 🌟 기프티콘 저장 또는 수정 함수
    // --------------------------------------------------

    /**
     * 폼 상태의 정보를 바탕으로 새로운 기프티콘을 저장하거나 기존 기프티콘을 수정합니다.
     */
    fun saveOrUpdateGiftcon() {
        viewModelScope.launch {
            _formState.update { it.copy(isSaving = true, saveError = null, saveSuccess = false, successMessage = null) }

            // 유효성 검사: 브랜드와 상품명은 필수
            if (_formState.value.brand.isBlank() || _formState.value.productName.isBlank()) {
                _formState.update {
                    it.copy(
                        isSaving = false,
                        saveError = "브랜드와 상품명을 입력해야 합니다."
                    )
                }
                return@launch
            }

            try {
                // 🌟 ID가 있으면 수정, 없으면 새로 추가
                val isUpdate = _currentGiftconId.value != null
                val giftconId = _currentGiftconId.value ?: "g${System.currentTimeMillis()}"

                // 기존 기프티콘의 사용 여부/사용일자를 유지하기 위해 찾음
                val existingGiftcon = _uiState.value.giftconList.find { it.id == giftconId }

                // 🌟 Giftcon 엔티티의 모든 필드를 명시적으로 지정하여 생성/수정 데이터 구성
                val processedGiftcon = Giftcon(
                    id = giftconId,
                    brand = _formState.value.brand,
                    productName = _formState.value.productName,
                    expiryDate = _formState.value.expiryDate,
                    price = _formState.value.price.toDoubleOrNull() ?: 0.0,
                    imageUrl = _formState.value.imageUrl,
                    // 수정 시에는 기존 사용 여부/사용일자를 유지
                    isUsed = existingGiftcon?.isUsed ?: false,
                    usedDate = existingGiftcon?.usedDate,
                    // 폼 상태에 있는 필드들
                    barcodeNumber = _formState.value.barcodeNumber,
                    memo = _formState.value.memo,
                    category = _formState.value.category,
                    // Geofencing 관련 필드는 폼에 없으므로 기존 값 유지 또는 기본값 사용
                    storeLatitude = existingGiftcon?.storeLatitude ?: 0.0,
                    storeLongitude = existingGiftcon?.storeLongitude ?: 0.0,
                    geofenceRadiusKm = existingGiftcon?.geofenceRadiusKm ?: 1.0
                )

                // TODO: Repository의 insert/update 함수 호출 (현재는 임시로 목록 업데이트)
                val currentList = _uiState.value.giftconList.toMutableList()
                if (isUpdate) {
                    // 수정: 기존 항목 제거 후 새 항목 추가
                    currentList.removeAll { it.id == giftconId }
                    currentList.add(0, processedGiftcon) // 수정된 항목을 맨 앞으로 이동
                } else {
                    // 저장: 새 항목 추가
                    currentList.add(0, processedGiftcon)
                }

                _uiState.update {
                    it.copy(giftconList = currentList, errorMessage = null)
                }

                // 🌟🌟🌟 핵심 수정: ViewModel이 성공 메시지를 결정하여 상태에 저장 🌟🌟🌟
                val successMessage = if (isUpdate) "기프티콘이 성공적으로 수정되었습니다!" else "기프티콘이 성공적으로 등록되었습니다!"

                // 성공 상태 업데이트 및 폼/ID 리셋
                _currentGiftconId.value = null // ID 리셋
                _formState.update {
                    GiftconFormState(
                        saveSuccess = true,
                        isSaving = false,
                        successMessage = successMessage // 🌟 메시지 전달
                    )
                }

            } catch (e: Exception) {
                _formState.update {
                    it.copy(
                        isSaving = false,
                        saveError = "저장 중 오류 발생: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * 기존 saveGiftcon() 함수는 saveOrUpdateGiftcon()을 호출하도록 변경합니다.
     */
    fun saveGiftcon() = saveOrUpdateGiftcon()


    /**
     * 폼 상태를 초기 상태로 리셋하고, 현재 수정 ID도 리셋합니다. (저장 후 또는 화면 진입 시)
     */
    fun resetFormState() {
        _formState.value = GiftconFormState()
        _currentGiftconId.value = null // ID 리셋 추가
    }

    // --------------------------------------------------
    // 임시 더미 데이터 (Giftcon.kt 엔티티의 모든 필드를 명시적으로 지정)
    // --------------------------------------------------
    private fun createDummyGiftcons(): List<Giftcon> {
        // Giftcon 엔티티의 모든 필드를 채워서 오류를 방지합니다.
        return listOf(
            Giftcon(
                id = "g1",
                brand = "스타벅스",
                productName = "아메리카노 Tall",
                expiryDate = Date(System.currentTimeMillis() + 86400000L * 30),
                price = 4500.0,
                isUsed = false,
                usedDate = null,
                imageUrl = null,
                barcodeNumber = "1234567890",
                memo = "아침에 마시기 좋음",
                category = "음료",
                storeLatitude = 37.5665,
                storeLongitude = 126.9780,
                geofenceRadiusKm = 0.5
            ),
            Giftcon(
                id = "g2",
                brand = "베스킨라빈스",
                productName = "파인트",
                expiryDate = Date(System.currentTimeMillis() + 86400000L * 10),
                price = 8900.0,
                isUsed = false,
                usedDate = null,
                imageUrl = null,
                barcodeNumber = "0987654321",
                memo = "주말에 사용 예정",
                category = "디저트",
                storeLatitude = 37.5015,
                storeLongitude = 127.0395,
                geofenceRadiusKm = 1.0
            ),
            Giftcon(
                id = "g3",
                brand = "CU",
                productName = "삼각김밥 교환권",
                expiryDate = Date(System.currentTimeMillis() - 86400000L * 5), // 만료됨
                price = 1500.0,
                isUsed = true,
                usedDate = Date(System.currentTimeMillis() - 86400000L * 10),
                imageUrl = null,
                barcodeNumber = "1122334455",
                memo = "급하게 사용함",
                category = "식사",
                storeLatitude = 37.4979,
                storeLongitude = 127.0276,
                geofenceRadiusKm = 1.5
            )
        )
    }
}
