package com.example.giftguard.entities

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import java.util.Date

/**
 * 기프티콘 정보를 담는 데이터 모델입니다.
 * Firestore 직렬화를 위해 모든 필드는 var로 선언하고 기본값을 제공하며,
 * snake_case API 명세에 맞추기 위해 @PropertyName을 사용합니다.
 */
data class Giftcon(
    @DocumentId
    val id: String = "",

    // 매핑이 필요 없는 필드
    val brand: String = "", // 브랜드명
    val price: Double = 0.0, // 금액
    val category: String = "기타", // 카테고리
    val memo: String = "", // 메모


    // snake_case 명세에 맞춘 매핑이 필요한 필드
    @PropertyName("product_name")
    val productName: String = "", // 상품명
    @PropertyName("barcode_number")
    val barcodeNumber: String = "", // 바코드 번호
    @PropertyName("expiry_date")
    val expiryDate: Date = Date(), // 유효기간
    @PropertyName("image_url")
    val imageUrl: String? = null, // 기프티콘 이미지 URL
    @PropertyName("is_used")
    val isUsed: Boolean = false, // 사용 여부
    @PropertyName("used_date")
    val usedDate: Date? = null, // 사용 일시

    // Geofencing 관련 필드
    val storeLatitude: Double = 0.0,
    val storeLongitude: Double = 0.0,
    val geofenceRadiusKm: Double = 1.0
)