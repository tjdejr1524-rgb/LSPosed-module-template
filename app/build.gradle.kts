plugins {
    id("com.android.application")
}

android {
    namespace = "com.mymodule.uuidhoo"
    compileSdk = 36 // Can be modified as needed

    defaultConfig {
        applicationId = "com.mymodule.uuidhoo"
        minSdk = 31
        versionCode = 1
        versionName = "1.0.0"
    }

    lint {
        targetSdk = 36
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    lint {
        checkReleaseBuilds = false
    }

    dependenciesInfo {
        includeInApk = false
    }
}
// app/build.gradle.kts
dependencies {
    // 기존 libxposed API (필요하다면 유지)
    compileOnly("io.github.libxposed:api:101.0.0") 

    // ★ 레거시 de.robv API 추가
    compileOnly("de.robv.android.xposed:api:82")
}
