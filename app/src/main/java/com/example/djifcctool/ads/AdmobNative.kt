package com.example.djifcctool.ads

import android.content.Context
import android.view.LayoutInflater
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.djifcctool.R
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import kotlinx.coroutines.delay

/**
 * Lädt + zeigt eine native AdMob-Anzeige (Erweiterte native Anzeige).
 * Verwendet die XML-Vorlage [R.layout.ad_native_advanced].
 * Pollt periodisch auf Consent, falls dieser erst nach erstem Compose erteilt wird.
 */
@Composable
fun AdmobNative(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var canShow by remember { mutableStateOf(ConsentManager.canRequestAds(context)) }

    LaunchedEffect(canShow) {
        if (!canShow) {
            while (!canShow) {
                delay(1000)
                if (ConsentManager.canRequestAds(context)) canShow = true
            }
        }
        if (canShow && nativeAd == null) {
            loadNativeAd(context) { ad -> nativeAd = ad }
        }
    }

    DisposableEffect(Unit) {
        onDispose { nativeAd?.destroy() }
    }

    val ad = nativeAd ?: return
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val inflater = LayoutInflater.from(ctx)
            val view = inflater.inflate(R.layout.ad_native_advanced, null) as NativeAdView
            populate(view, ad)
            view
        },
        update = { populate(it as NativeAdView, ad) }
    )
}

private fun loadNativeAd(context: Context, onLoaded: (NativeAd) -> Unit) {
    AdLoader.Builder(context, AdsConfig.NATIVE_ID)
        .forNativeAd { ad -> onLoaded(ad) }
        .build()
        .loadAd(ConsentManager.buildAdRequest(context))
}

private fun populate(adView: NativeAdView, nativeAd: NativeAd) {
    adView.findViewById<android.widget.TextView>(R.id.ad_headline)?.also {
        it.text = nativeAd.headline
        adView.headlineView = it
    }
    adView.findViewById<android.widget.TextView>(R.id.ad_body)?.also {
        it.text = nativeAd.body ?: ""
        adView.bodyView = it
    }
    adView.findViewById<android.widget.Button>(R.id.ad_call_to_action)?.also {
        it.text = nativeAd.callToAction ?: ""
        adView.callToActionView = it
    }
    adView.setNativeAd(nativeAd)
}
