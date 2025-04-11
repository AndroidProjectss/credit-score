plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.credit_score"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.credit_score"
        minSdk = 25
        targetSdk = 35
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
    packaging {
        resources {
            excludes.add("META-INF/INDEX.LIST")
            excludes.add("META-INF/DEPENDENCIES")
            excludes.add("META-INF/LICENSE.txt")
            excludes.add("META-INF/license.txt")
            excludes.add("META-INF/NOTICE.txt")
            excludes.add("META-INF/notice.txt")
            excludes.add("META-INF/ASL2.0")
            excludes.add("META-INF/*.proto")
            excludes.add("META-INF/*.kotlin_module")
            excludes.add("META-INF/MANIFEST.MF")

            pickFirsts.add("META-INF/io.netty.versions.properties")
            pickFirsts.add("META-INF/jersey-module-version")
            pickFirsts.add("META-INF/services/javax.annotation.processing.Processor")
        }
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")

    implementation ("com.android.volley:volley:1.2.1")

    // Добавляем Retrofit для сетевых запросов
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation ("com.squareup.retrofit2:converter-scalars:2.9.0")
    // GSON для работы с JSON
    implementation("com.google.code.gson:gson:2.10.1")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.5")

    // Добавляем Retrofit для сетевых запросов
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation ("com.squareup.retrofit2:converter-scalars:2.9.0")
    // GSON для работы с JSON
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.github.anastr:speedviewlib:1.6.0")
    // CameraX dependencies
    val cameraxVersion = "1.2.3"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // Google Cloud Vision API
    implementation("com.google.auth:google-auth-library-oauth2-http:1.15.0")
    implementation("com.google.cloud:google-cloud-vision:3.7.0") {
        exclude(group = "org.conscrypt", module = "conscrypt-openjdk-uber")
    }
    implementation("com.google.api:gax-grpc:2.22.0") {
        exclude(group = "org.conscrypt", module = "conscrypt-openjdk-uber")
    }

    // gRPC - выбираем одну согласованную версию
    implementation("io.grpc:grpc-okhttp:1.53.0")
    implementation("io.grpc:grpc-android:1.53.0")
    implementation("io.grpc:grpc-protobuf:1.53.0") {
        exclude(group = "com.google.protobuf", module = "protobuf-lite")
    }
    implementation("io.grpc:grpc-stub:1.53.0")

    // For executing background tasks
    implementation("androidx.concurrent:concurrent-futures:1.1.0")

    // For material design components
    implementation("com.google.android.material:material:1.8.0")

    // Glide для работы с изображениями
    implementation("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")

    // Permissions
    implementation("com.karumi:dexter:6.2.3")
    // ML Kit Face Detection

    // ML Kit Face Recognition (для сравнения лиц)
    implementation ("com.google.mlkit:face-detection:16.1.5")
    implementation ("androidx.camera:camera-camera2:1.2.3")
    implementation ("androidx.camera:camera-lifecycle:1.2.3")
    implementation ("androidx.camera:camera-view:1.2.3")

    // CameraX
    implementation ("androidx.camera:camera-camera2:1.2.2")
    implementation ("androidx.camera:camera-lifecycle:1.2.2")
    implementation ("androidx.camera:camera-view:1.2.2")
    implementation ("androidx.exifinterface:exifinterface:1.3.6")
}