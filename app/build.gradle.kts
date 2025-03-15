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

    // ✅ Firebase BOM kullanarak tüm Firebase sürümlerini uyumlu hale getiriyoruz
    implementation (libs.firebase.firestore.ktx.v2481)
    implementation (libs.socket.socket.io.client)
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    // ✅ Firebase (Tekrar edenleri kaldırdık)
    implementation(libs.com.google.firebase.firebase.auth.ktx)
    implementation(libs.google.firebase.firestore.ktx)
    implementation(libs.google.firebase.database.ktx)

    // ✅ Firebase UI Auth (Opsiyonel)
    implementation(libs.firebase.ui.auth.v802)
    implementation (libs.firebase.functions.ktx)
    // ✅ Hilt
    implementation(libs.hilt.android.v2511)
    kapt(libs.hilt.android.compiler)

    // ✅ Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation( libs.androidx.room.ktx)
    // ✅ Material Design (Tekrar eden kaldırıldı)
    implementation(libs.material.v180)
    implementation (libs.androidx.room.runtime)
    kapt (libs.androidx.room.compiler)

    // ✅ AndroidX
    implementation("androidx.core:core-ktx:1.10.1")
    implementation(libs.androidx.appcompat.v161)
    implementation(libs.androidx.activity.ktx)
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // ✅ Testler
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
