package com.example.djifcctool.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import java.util.Date

/**
 * Zeigt einmal pro App-Vordergrund eine App-Open-Werbung.
 * Initialisiert sich beim Application#onCreate, nutzt Process-Lifecycle,
 * um „App rückt in den Vordergrund" zu erkennen.
 */
class AppOpenAdManager(private val application: Application) :
    Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private companion object { const val TAG = "AppOpenAdManager" }

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var loadTime: Long = 0
    private var currentActivity: Activity? = null

    fun register() {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && (Date().time - loadTime) < 4 * 60 * 60 * 1000L
    }

    fun loadAd() {
        if (isLoadingAd || isAdAvailable()) return
        if (!ConsentManager.canRequestAds(application)) return
        isLoadingAd = true
        AppOpenAd.load(
            application,
            AdsConfig.APP_OPEN_ID,
            ConsentManager.buildAdRequest(application),
            object : AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                }
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAd = false
                    Log.w(TAG, "AppOpen load failed: ${loadAdError.message}")
                }
            }
        )
    }

    private fun showAdIfAvailable() {
        val activity = currentActivity ?: return
        if (isShowingAd) return
        if (!isAdAvailable()) { loadAd(); return }
        val ad = appOpenAd ?: return
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                loadAd()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                appOpenAd = null
                isShowingAd = false
                loadAd()
            }
            override fun onAdShowedFullScreenContent() { isShowingAd = true }
        }
        ad.show(activity)
    }

    // Process Lifecycle: app-foregrounded
    override fun onStart(owner: LifecycleOwner) {
        if (!isShowingAd) showAdIfAvailable()
    }

    // ActivityLifecycleCallbacks
    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {}
    override fun onActivityStarted(activity: Activity) { if (!isShowingAd) currentActivity = activity }
    override fun onActivityResumed(activity: Activity) { currentActivity = activity }
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) { if (currentActivity === activity) currentActivity = null }
}
