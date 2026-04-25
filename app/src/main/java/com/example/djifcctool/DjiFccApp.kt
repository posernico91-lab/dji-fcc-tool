package com.example.djifcctool

import android.app.Application
import com.example.djifcctool.ads.AdsManager
import com.example.djifcctool.ads.AppOpenAdManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DjiFccApp : Application() {

    lateinit var appOpenAdManager: AppOpenAdManager
        private set

    override fun onCreate() {
        super.onCreate()
        appOpenAdManager = AppOpenAdManager(this)
        appOpenAdManager.register()
        // SDK wird erst nach erteiltem Consent in MainActivity initialisiert.
    }

    fun onConsentReady() {
        AdsManager.initializeIfAllowed(this)
        appOpenAdManager.loadAd()
    }
}
