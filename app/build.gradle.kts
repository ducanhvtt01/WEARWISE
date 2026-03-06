plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
}

android {
    namespace = "com.example.dacs3"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.dacs3"
        minSdk = 30
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.datastore.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")
    implementation("com.airbnb.android:lottie-compose:6.4.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // Thư viện ViewModel cho Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Supabase
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.5.0") // Thao tác Database
    implementation("io.github.jan-tennert.supabase:gotrue-kt:2.5.0")    // Đăng nhập (Auth)
    implementation("io.github.jan-tennert.supabase:storage-kt:2.5.0")   // Lưu ảnh quần áo
    implementation("io.github.jan-tennert.supabase:realtime-kt:2.5.0")
    implementation("io.github.jan-tennert.supabase:compose-auth:2.5.0")

    // Serialization (Để chuyển đổi JSON sang Class)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Thư viện hỗ trợ mạng (Ktor)
    implementation("io.ktor:ktor-client-android:2.3.10")

    // Thư viện hỗ trợ định vị vị trí người dùng
    implementation("com.google.android.gms:play-services-location:21.1.0")

    // Thư viện lưu trạng thái giao diện của ứng dụng
    implementation("androidx.datastore:datastore-preferences:1.0.0")
}