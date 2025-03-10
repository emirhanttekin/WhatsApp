

pluginManagement {
    repositories {
        google()  // ✅ Google Repository olmalı
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("androidx.navigation.safeargs") version "2.8.8" apply false



    }

}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()  // Google Maven deposu eklenmeli
        mavenCentral()
    }
}


rootProject.name = "WhatsApp"
include(":app")
