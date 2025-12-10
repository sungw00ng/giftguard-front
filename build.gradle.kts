// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // AGP & Kotlin 버전 통일
    id("com.android.application") version "8.9.1" apply false
    id("com.android.library") version "8.9.1" apply false

    kotlin("android") version "2.2.20" apply false
    kotlin("plugin.serialization") version "2.2.20" apply false
    id("com.google.gms.google-services") version "4.4.0" apply false // <-- 이 라인을 추가해야 합니다.

    // Firebase Google Services 플러그인 쓰면 주석 해제 + app 모듈에도 적용
    // id("com.google.gms.google-services") version "4.4.2" apply false
}
