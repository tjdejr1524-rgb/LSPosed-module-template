pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // ★ Xposed API 저장소 추가
        maven { url = uri("https://api.xposed.info/") }
    }
}

rootProject.name = "XposedModuleTemplate"
include(":app")
includeBuild("libxposed-api")
