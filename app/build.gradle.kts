plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("androidx.navigation.safeargs.kotlin") version "2.7.7"
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
            excludes.addAll(
                listOf(
                    "META-INF/INDEX.LIST",
                    "META-INF/DEPENDENCIES",
                    "META-INF/LICENSE.txt",
                    "META-INF/license.txt",
                    "META-INF/NOTICE.txt",
                    "META-INF/notice.txt",
                    "META-INF/ASL2.0",
                    "META-INF/*.proto",
                    "META-INF/*.kotlin_module",
                    "META-INF/MANIFEST.MF"
                )
            )
            pickFirsts.addAll(
                listOf(
                    "META-INF/io.netty.versions.properties",
                    "META-INF/jersey-module-version",
                    "META-INF/services/javax.annotation.processing.Processor"
                )
            )
        }
    }

    kotlin {
        jvmToolchain(11)
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Core dependencies
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.android.volley:volley:1.2.1")

    // Retrofit for network requests
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // AndroidX dependencies (remove duplicates, prefer versions from libs.versions.toml)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Navigation Component (use consistent versions)
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Charts and UI components
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.github.anastr:speedviewlib:1.6.0")

    // CameraX
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

    // gRPC
    implementation("io.grpc:grpc-okhttp:1.53.0")
    implementation("io.grpc:grpc-android:1.53.0")
    implementation("io.grpc:grpc-protobuf:1.53.0") {
        exclude(group = "com.google.protobuf", module = "protobuf-lite")
    }
    implementation("io.grpc:grpc-stub:1.53.0")

    // Background tasks
    implementation("androidx.concurrent:concurrent-futures:1.1.0")

    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")

    // Permissions
    implementation("com.karumi:dexter:6.2.3")

    // ML Kit Face Detection
    implementation("com.google.mlkit:face-detection:16.1.5")

    // ExifInterface
    implementation("androidx.exifinterface:exifinterface:1.3.6")
}