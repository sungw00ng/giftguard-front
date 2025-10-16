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
