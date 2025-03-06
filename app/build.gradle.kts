plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android") // ✅ Kotlin plugin doğru yazıldı!
    id("kotlin-kapt") // ✅ Kapt doğru kaldı!
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.whatsapp"
    compileSdk = 35  // Burayı güncelle

    defaultConfig {
        applicationId = "com.example.whatsapp"
        minSdk = 24
        targetSdk = 34  // Burayı güncelle
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}


dependencies {


    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-android-compiler:2.51.1")
    // Firebase Authentication (Sadece en güncel sürüm eklenmeli)
    implementation(libs.google.firebase.auth.ktx)

    // Eğer FirebaseUI kullanıyorsan, sürümü Firebase sürümüne uygun olmalı
    implementation(libs.firebase.ui.auth.v802)

    // Diğer Firebase bağımlılıkları
    implementation(libs.firebase.firestore.ktx.v24100)
    implementation(libs.firebase.database.ktx.v2030)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.database.ktx)
    implementation (libs.androidx.navigation.fragment.ktx)
    implementation (libs.androidx.navigation.ui.ktx)
    implementation (libs.hilt.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}