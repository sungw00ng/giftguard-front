| 파일/폴더                                | 역할 (What it does)                       | 계층 (Layer)            |
| ------------------------------------ | --------------------------------------- | --------------------- |
| **`MainActivity.kt`**                | 앱 진입점. Compose 설정 + AppNavigation 호출    | Presentation (최상위)    |
| **`data/AppContainer.kt`**           | 의존성 주입 (DI). Repository 등 객체 생성 관리      | Data/Domain (인프라)     |
| **`data/GiftconRepository.kt`**      | Firestore 등과의 데이터 처리 담당. ViewModel에서 사용 | Data                  |
| **`entities/Giftcon.kt`**            | 기프티콘 모델 정의 (id, 브랜드명, 유효기간 등)           | Domain                |
| **`layout/`**                        | 전반적 UI 스타일 정의 (색상, 테마, 레이아웃 등)          | Presentation (UI 스타일) |
| └ `AppThemeColors.kt`                | 앱 색상 정의                                 | Presentation          |
| └ `MainLayout.kt`                    | 공통 레이아웃 (Scaffold) 구조                   | Presentation          |
| └ `Theme.kt`, `Type.kt`              | Material 디자인의 색상, 폰트 정의                 | Presentation          |
| **`navigation/`**                    | 화면 간 이동 정의                              | Presentation (UI 로직)  |
| └ `AppNavigation.kt`                 | NavHost 설정 및 화면 연결                      | Presentation          |
| └ `Screen.kt`                        | 각 화면의 route 정의 상수                       | Presentation          |
| **`screens/`**                       | 사용자에게 보여지는 UI 화면들                       | Presentation (UI 구현)  |
| └ `AddGifticonScreen.kt`             | 기프티콘 등록/수정 화면                           | Presentation          |
| └ `DashboardScreen.kt`               | 메인 화면 (보관함) / 리스트                       | Presentation          |
| └ `DetailScreen.kt`                  | 기프티콘 상세 정보                              | Presentation          |
| └ `StoreMapScreen.kt`                | 매장 지도 보기 화면                             | Presentation          |
| └ `GeofenceAlertScreen.kt`           | 위치 기반 알림 설정 화면                          | Presentation          |
| **`Viewmodels/GiftconViewModel.kt`** | UI 상태 관리 + 비즈니스 로직 수행                   | Presentation          |


## back 연결 통로
| 구분     | 현재 개발된 파일                      | 백엔드 연동 시 역할                                     |
| ------ | ------------------------------ | ----------------------------------------------- |
| 핵심 통로  | data/GiftconRepository.kt      | 이 파일의 구현을 API 호출로 변경                        |
| 데이터 모델 | entities/Giftcon.kt            | 백엔드 API의 JSON 응답 포맷과 일치하도록 정의               |
| 상태 관리  | viewmodels/GiftconViewModel.kt | Repository를 통해 백엔드 API를 호출하고, 결과를 UI 상태로 변환 |

```Kotlin
// 가상의 로컬 데이터 CRUD
suspend fun saveGiftcon(giftcon: Giftcon) { /* ... 로컬 DB에 저장 */ }
// ...
```

```Kotlin
// 백엔드 API 통신
suspend fun saveGiftcon(giftcon: Giftcon) {
    //Ktor Client 등을 사용하여 HTTP POST 요청
    apiService.post("/giftcons", giftcon.toJson())
}
// ...
```

| 단계                 | 파일명                            | 역할 및 설명                                                 |
| ------------------ | ------------------------------ | ------------------------------------------------------- |
| 1. Ktor API 서비스 설정 | data/GiftconApiService.kt      | Ktor HTTP Client 설정 및 서버 엔드포인트에 대응하는 API 호출 함수 정의       |
| 2. Repository 수정   | data/GiftconRepository.kt      | 기존 로컬 저장소 대신 Ktor API 호출로 데이터 처리 구현 변경                  |
| 3. 의존성 주입 업데이트     | data/AppContainer.kt           | Ktor Client 인스턴스를 생성하여 GiftconRepository에 주입하도록 변경      |
| 4. ViewModel 업데이트  | viewmodels/GiftconViewModel.kt | barcode, memo 필드 추가 및 Ktor API 호출 결과를 반영하도록 상태 관리 로직 수정 |
| 5. 엔티티 직렬화 설정      | entities/Giftcon.kt            | Ktor 직렬화를 위해 `@Serializable` 어노테이션 추가                   |
