// Project-level Gradle Configuration:build.gradle.kts

plugins {
    // Standard plugins defined in the version catalog
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}

// ⚠️ 이전에 있었던 allprojects { repositories { ... } } 블록을 제거했습니다.
// 저장소 설정은 이제 settings.gradle.kts에서 처리됩니다.
