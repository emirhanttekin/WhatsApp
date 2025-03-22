plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    id("androidx.navigation.safeargs.kotlin") // 🔥 Safe Args burada doğru!

}

android {
    namespace = "com.example.whatsapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.whatsapp"
        minSdk = 24
        targetSdk = 34
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
    val nav_version = "2.8.8"
    val firebase_bom_version = "33.0.0"
    val glide_version = "4.15.1"

    // ✅ Firebase BOM (Tüm Firebase sürümlerini uyumlu hale getiriyoruz)
    implementation(platform("com.google.firebase:firebase-bom:$firebase_bom_version"))
    implementation(libs.com.google.firebase.firebase.auth.ktx2)
    implementation(libs.com.google.firebase.firebase.firestore.ktx)
    implementation(libs.com.google.firebase.firebase.database.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.google.firebase.functions.ktx)
    implementation(libs.firebase.ui.auth.v802)
    implementation(libs.androidx.core.ktx.v1120)
    // ✅ Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation ("androidx.lifecycle:lifecycle-process:2.6.2")

    // ✅ Hilt (Dependency Injection)
    implementation(libs.hilt.android.v2511)
    kapt(libs.hilt.android.compiler)

    // ✅ Room (SQLite için)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt("androidx.room:room-compiler:2.6.1")

    // ✅ Glide (Resim Yükleme)
    implementation(libs.glide.v4151)
    kapt("com.github.bumptech.glide:compiler:$glide_version")

    // ✅ UCrop (Resim Kırpma)
    implementation(libs.ucropVersion)
    implementation (libs.androidx.cardview)
    // ✅ AndroidX Güncellemeleri
    implementation(libs.androidx.core.ktx.v1130)
    implementation(libs.androidx.appcompat.v161)
    implementation(libs.androidx.activity.ktx.v190)
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation (libs.material.v1110) // En son sürüm
    // ✅ Socket.IO (Gerçek Zamanlı Bağlantı)
    implementation(libs.socket.socket.io.client)

    // ✅ Testler
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit.v115)
    androidTestImplementation(libs.androidx.espresso.core.v351)
}
