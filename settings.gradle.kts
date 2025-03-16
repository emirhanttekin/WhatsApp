

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
        google()
        mavenCentral()
        maven("https://jitpack.io") // JitPack'in doğru eklenmesi
    }
}



rootProject.name = "WhatsApp"
include(":app")
