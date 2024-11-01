plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.track_mate"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.track_mate"
        minSdk = 22
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.biometric:biometric:1.1.0")
    implementation ("com.airbnb.android:lottie:6.0.1")
    implementation ("androidx.core:core:1.10.0")
    implementation ("com.itextpdf:itext7-core:7.1.16")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
