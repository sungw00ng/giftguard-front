plugins {
    // 2단계(프로젝트 레벨)에서 정의한 플러그인 별칭을 적용합니다.
    alias(libs.plugins.android.application)
    // 1. 구문 수정: 표준 Kotlin Android 플러그인 별칭을 사용합니다.
    alias(libs.plugins.kotlin.android)
    // 2. 오류 해결: 이 플러그인이 프로젝트 수준 build.gradle.kts에 정의되지 않아 발생한 오류(UnknownPluginException)를 해결하기 위해 주석 처리합니다.
    // Firebase를 활성화하려면 프로젝트 수준의 settings.gradle.kts 파일에도 해당 플러그인의 버전을 정의해야 합니다.
    // id("com.google.gms.google-services")
}

android {
    // Canvas에 제공된 Kotlin 파일의 패키지 이름인 "com.example.giftguard"를 사용합니다.
    namespace = "com.example.giftguard"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.giftguard"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    // 🌟 컴파일 옵션 최신화 (Kotlin 컴파일러와 일치)
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        // Jetpack Compose를 사용하도록 활성화합니다.
        compose = true
    }

    // 🌟 Compose Compiler 설정
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // -----------------------------------------------------------------
    // 🌟 BOM (Bill of Materials)
    // -----------------------------------------------------------------
    // Compose BOM을 사용하여 모든 Compose 라이브러리의 버전을 통합 관리합니다.
    implementation(platform(libs.androidx.compose.bom))

    // Firebase BOM을 사용하여 모든 Firebase 라이브러리의 버전을 통합 관리합니다.
    // 플러그인은 주석 처리했지만, 의존성 라이브러리는 그대로 유지합니다.
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))


    // -----------------------------------------------------------------
    // 🌟 핵심 Compose 및 Androidx 라이브러리
    // -----------------------------------------------------------------
    // 필수 기본 라이브러리
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // 🌟 오류 해결: themes.xml에서 Theme.AppCompat.* 테마를 참조할 수 있도록 의존성 추가
    implementation("androidx.appcompat:appcompat:1.6.1")

    // Compose UI 및 그래픽
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)

    // 내비게이션
    implementation(libs.androidx.navigation.compose)

    // Material 3
    implementation(libs.androidx.material3)

    // 🌟 확장 아이콘: NotificationsActive 아이콘 사용을 위한 확장 아이브러리
    implementation("androidx.compose.material:material-icons-extended")


    // -----------------------------------------------------------------
    // 🌟 Firebase 라이브러리
    // -----------------------------------------------------------------
    // Firestore 의존성 (Giftcon 모델 직렬화/역직렬화용)
    implementation("com.google.firebase:firebase-firestore-ktx")
    // Firebase Auth 의존성 (사용자 인증용 - 필수)
    implementation("com.google.firebase:firebase-auth-ktx")

    // -----------------------------------------------------------------
    // 🌟 테스트 라이브러리 (옵션) - 빨간 줄 오류 해결을 위해 문자열로 변경
    // -----------------------------------------------------------------
    // 단위 테스트 프레임워크
    testImplementation("junit:junit:4.13.2")

    // AndroidX 테스트 라이브러리 (JUnit)
    androidTestImplementation("androidx.test.ext:junit:1.1.5")

    // Espresso UI 테스트 라이브러리
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Compose UI 테스트 (JUnit4)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // Compose 도구: 디버깅 및 미리보기 (debugImplementation)
    debugImplementation("androidx.compose.ui:ui-tooling")
    // Compose 테스트 매니페스트 (debugImplementation)
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
