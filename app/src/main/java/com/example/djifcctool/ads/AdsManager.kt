package com.example.djifcctool.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Zentraler Ad-Manager. Initialisiert das Mobile Ads SDK NUR nach
 * erteilter Zustimmung, hält das aktuelle Interstitial vor und feuert
 * alle [AdsConfig.INTERSTITIAL_INTERVAL_MS] (3 Min.) ein H5/Video-Interstitial
 * im Vordergrund.
 */
object AdsManager {
    private const val TAG = "AdsManager"
    private val sdkInitialized = AtomicBoolean(false)
    private val handler = Handler(Looper.getMainLooper())

    @Volatile private var interstitial: InterstitialAd? = null
    @Volatile private var loadingInterstitial = false
    @Volatile private var lastInterstitialShownAt: Long = 0L
    @Volatile private var currentActivityRef: Activity? = null
    @Volatile private var rotatorScheduled = false

    /** Aufrufen, sobald [ConsentManager.canRequestAds] true ist. */
    fun initializeIfAllowed(app: Application) {
        if (!ConsentManager.canRequestAds(app)) {
            Log.i(TAG, "SDK init skipped — no consent yet")
            return
        }
        if (!sdkInitialized.compareAndSet(false, true)) return

        // Empfohlene Konfig: keine User-Tracking-IDs ohne Einwilligung,
        // jugendfreundlich (PG, MA fern halten).
        val cfg = RequestConfiguration.Builder()
            .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_PG)
            .build()
        MobileAds.setRequestConfiguration(cfg)

        MobileAds.initialize(app) {
            Log.i(TAG, "MobileAds initialized")
            ConsentManager.markInitialized()
            preloadInterstitial(app)
            scheduleInterstitialRotator(app)
        }
    }

    fun onActivityResumed(activity: Activity) {
        currentActivityRef = activity
    }

    fun onActivityPaused(activity: Activity) {
        if (currentActivityRef === activity) currentActivityRef = null
    }

    // ----- Interstitial ------------------------------------------------------

    private fun preloadInterstitial(context: Context) {
        if (interstitial != null || loadingInterstitial) return
        if (!sdkInitialized.get()) return
        loadingInterstitial = true
        InterstitialAd.load(
            context,
            AdsConfig.INTERSTITIAL_ID,
            ConsentManager.buildAdRequest(context),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    loadingInterstitial = false
                    Log.d(TAG, "Interstitial preloaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitial = null
                    loadingInterstitial = false
                    Log.w(TAG, "Interstitial load failed: ${error.message}")
                    // Backoff: nächster Versuch in 30 s.
                    handler.postDelayed({ preloadInterstitial(context) }, 30_000L)
                }
            }
        )
    }

    private fun scheduleInterstitialRotator(app: Application) {
        if (rotatorScheduled) return
        rotatorScheduled = true
        val tick = object : Runnable {
            override fun run() {
                tryShowInterstitial(app)
                handler.postDelayed(this, AdsConfig.INTERSTITIAL_INTERVAL_MS)
            }
        }
        // Erster Slot frühestens nach dem Intervall, nicht direkt beim Start
        // (App-Open hat bereits feuert).
        handler.postDelayed(tick, AdsConfig.INTERSTITIAL_INTERVAL_MS)
    }

    private fun tryShowInterstitial(app: Application) {
        val act = currentActivityRef ?: return // App im Hintergrund → nichts tun
        val ad = interstitial
        val now = System.currentTimeMillis()
        // Sicherheits-Throttle: niemals häufiger als alle ~60 Sekunden
        if (now - lastInterstitialShownAt < 60_000L) return
        if (ad == null) {
            preloadInterstitial(app)
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitial = null
                lastInterstitialShownAt = System.currentTimeMillis()
                preloadInterstitial(app)
            }
            override fun onAdFailedToShowFullScreenContent(p0: com.google.android.gms.ads.AdError) {
                interstitial = null
                preloadInterstitial(app)
            }
        }
        ad.show(act)
    }

    /** Nicht-zerstörendes Re-Trigger nach Konfiguration. */
    fun ensureInterstitialPreloaded(context: Context) = preloadInterstitial(context)
}
