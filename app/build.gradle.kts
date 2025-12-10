plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)

    id("com.google.gms.google-services")

}

android {
    namespace = "com.example.giftguard_login"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.giftguard_login"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        // 여긴 경고만 뜨는 부분이라 당장은 이렇게 둬도 됨
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        // 👉 Compose 안 쓸 거라면 아예 끄는 게 깔끔
        // compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    // -----------------------------
    // Retrofit / OkHttp
    // -----------------------------
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // -----------------------------
    // Kotlinx Serialization & Coroutines
    // -----------------------------
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // -----------------------------
    // Lifecycle
    // -----------------------------
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    // -----------------------------
    // Firebase / Google 로그인
    // -----------------------------
    implementation(platform("com.google.firebase:firebase-bom:32.7.4"))
    implementation("com.google.firebase:firebase-auth:22.3.1")
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // -----------------------------
    // 지도 / 위치 / 브라우저
    // -----------------------------
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("androidx.browser:browser:1.8.0")
    implementation("com.google.maps.android:android-maps-utils:3.4.0")
    implementation("com.google.maps.android:android-maps-utils")
    // -----------------------------
    // ML Kit (OCR)
    // -----------------------------
    implementation("com.google.mlkit:text-recognition:16.0.0")
    implementation("com.google.mlkit:text-recognition-korean:16.0.0")
    implementation("com.google.mlkit:vision-common:17.3.0")

    // -----------------------------
    // AndroidX 기본 UI (버전 카탈로그에서 오는 것들)
    // -----------------------------
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Activity-ktx (조금 더 편한 API)
    implementation("androidx.activity:activity-ktx:1.9.2")

    // -----------------------------
    // 테스트
    // -----------------------------
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("com.google.android.libraries.places:places:4.3.1")



}
