package com.example.giftguard.navigation

/**
 * 앱 내 모든 화면의 경로(Route)를 정의하는 Sealed Class입니다.
 * 이를 통해 타입 안정성을 확보하고 경로를 중앙에서 관리합니다.
 */
sealed class Screen(val route: String) {
    // --------------------------------------------------
    // 메인 네비게이션 경로
    // --------------------------------------------------
    object Dashboard : Screen("dashboard")        // 홈/기프티콘 목록

    // 🌟 최종 수정 🌟: AddGifticon을 인자 있는 경로 하나로 통일
    // route 자체를 인자를 포함하도록 정의합니다.
    object AddGifticon : Screen("add_gifticon/{giftconId}") {

        // 1. 등록 모드 (FAB 클릭): ID 인자로 "null" 문자열을 포함하여 경로를 생성합니다.
        fun createRoute() = "add_gifticon/null"

        // 2. 수정 모드 (상세 화면에서 호출): 실제 ID를 포함하여 경로를 생성합니다.
        fun createRoute(giftconId: String) = "add_gifticon/$giftconId"

        // routeWithArgs 대신 route를 사용하여 NavHost에 등록합니다.
        val routeWithArgs = route
    }

    object StoreMap : Screen("store_map")        // 매장 지도
    object GeofenceAlert : Screen("geofence_alert") // 위치 알림 관리

    // --------------------------------------------------
    // 상세 화면 및 기타 경로
    // --------------------------------------------------
    object Detail : Screen("gifticon_detail/{giftconId}") {
        fun createRoute(giftconId: String) = "gifticon_detail/$giftconId"
    }
}
