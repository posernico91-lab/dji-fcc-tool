package com.example.djifcctool.ads

/**
 * Zentrale AdMob-Konfiguration. Alle IDs der App
 * „DJI Mini FCC Switch" (Publisher 3778451046949775).
 */
object AdsConfig {
    const val APP_ID         = "ca-app-pub-3778451046949775~8364991629"
    const val APP_OPEN_ID    = "ca-app-pub-3778451046949775/3281764911"
    const val BANNER_ID      = "ca-app-pub-3778451046949775/1596403295"
    const val NATIVE_ID      = "ca-app-pub-3778451046949775/7778668269"
    const val INTERSTITIAL_ID = "ca-app-pub-3778451046949775/4594846583"

    /** Interstitial-Intervall in Millisekunden (alle 3 Minuten). */
    const val INTERSTITIAL_INTERVAL_MS: Long = 3 * 60 * 1000L
}
