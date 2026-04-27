package com.example.djifcctool.ads

import com.example.djifcctool.BuildConfig

/**
 * Zentrale AdMob-Konfiguration.
 * IDs kommen aus BuildConfig — Debug nutzt automatisch Google-Test-Anzeigen-IDs,
 * Release nutzt die Produktions-IDs des Publishers 3778451046949775.
 */
object AdsConfig {
    /** App-ID aus AndroidManifest meta-data (in beiden Varianten gleich). */
    const val APP_ID         = "ca-app-pub-3778451046949775~8364991629"

    val APP_OPEN_ID: String     = BuildConfig.AD_APP_OPEN_ID
    val BANNER_ID: String       = BuildConfig.AD_BANNER_ID
    val NATIVE_ID: String       = BuildConfig.AD_NATIVE_ID
    val INTERSTITIAL_ID: String = BuildConfig.AD_INTERSTITIAL_ID

    /** Interstitial-Intervall in Millisekunden (alle 3 Minuten). */
    const val INTERSTITIAL_INTERVAL_MS: Long = 3 * 60 * 1000L
}
