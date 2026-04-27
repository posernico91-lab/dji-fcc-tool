plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.example.djifcctool"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.poserpy.rangeboost"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("../rangeboost-release.jks")
            storePassword = "RangeBoost2026!"
            keyAlias = "rangeboost"
            keyPassword = "RangeBoost2026!"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Echte Produktions-IDs (Fill nur, sobald App im AdMob-Konto registriert + freigeschaltet)
            buildConfigField("String", "AD_APP_OPEN_ID",   "\"ca-app-pub-3778451046949775/3281764911\"")
            buildConfigField("String", "AD_BANNER_ID",     "\"ca-app-pub-3778451046949775/1596403295\"")
            buildConfigField("String", "AD_NATIVE_ID",     "\"ca-app-pub-3778451046949775/7778668269\"")
            buildConfigField("String", "AD_INTERSTITIAL_ID","\"ca-app-pub-3778451046949775/4594846583\"")
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            // Offizielle Google-Test-Anzeigen-IDs: liefern IMMER eine Test-Anzeige
            // Quelle: https://developers.google.com/admob/android/test-ads
            buildConfigField("String", "AD_APP_OPEN_ID",   "\"ca-app-pub-3940256099942544/9257395921\"")
            buildConfigField("String", "AD_BANNER_ID",     "\"ca-app-pub-3940256099942544/9214589741\"")
            buildConfigField("String", "AD_NATIVE_ID",     "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "AD_INTERSTITIAL_ID","\"ca-app-pub-3940256099942544/1033173712\"")
        }
    }

    bundle {
        language { enableSplit = true }
        density  { enableSplit = true }
        abi      { enableSplit = true }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
    packaging {
        resources.excludes += setOf("META-INF/AL2.0", "META-INF/LGPL2.1")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui:1.6.2")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.2")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-process:2.7.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-compiler:2.50")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // USB-Serial-Treiber (CDC-ACM) für DJI N1/N2 Remote — verifizierte Bytes-Quelle
    implementation("com.github.mik3y:usb-serial-for-android:3.7.0")

    // Google Mobile Ads SDK (AdMob) + User Messaging Platform (GDPR/IAB TCF)
    implementation("com.google.android.gms:play-services-ads:23.0.0")
    implementation("com.google.android.ump:user-messaging-platform:2.2.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.2")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.2")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.6.2")
}
