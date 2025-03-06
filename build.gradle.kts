// Top-level build file where you can add configuration options common to all sub-projects/modules.
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.5.0" apply false  // ✅ En güncel AGP sürümü
    id("com.android.library") version "8.5.0" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false  // ✅ Kotlin 2.1.0 Güncellendi!
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}
