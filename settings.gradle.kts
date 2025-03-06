pluginManagement {
    repositories {
        google()  // ✅ Google Repository olmalı
        mavenCentral()
        gradlePluginPortal()
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
