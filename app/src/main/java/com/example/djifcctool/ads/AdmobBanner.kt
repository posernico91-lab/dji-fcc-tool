package com.example.djifcctool.ads

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Compose-Wrapper für ein adaptives AdMob-Banner.
 * Wird nur angezeigt, sobald [ConsentManager.canRequestAds] erfüllt ist.
 */
@Composable
fun AdmobBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var canShow by remember { mutableStateOf(ConsentManager.canRequestAds(context)) }

    if (!canShow) {
        // Re-evaluieren nach kurzem Delay, falls sich Consent während
        // der Lebenszeit der Composable ändert.
        DisposableEffect(Unit) {
            val newValue = ConsentManager.canRequestAds(context)
            if (newValue != canShow) canShow = newValue
            onDispose { }
        }
        return
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx -> createBanner(ctx) }
    )
}

private fun createBanner(context: Context): View {
    val container = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }
    val adView = AdView(context).apply {
        adUnitId = AdsConfig.BANNER_ID
        setAdSize(adaptiveSize(context))
    }
    container.addView(adView)
    adView.loadAd(ConsentManager.buildAdRequest(context))
    return container
}

private fun adaptiveSize(context: Context): AdSize {
    val displayMetrics = context.resources.displayMetrics
    val widthDp = (displayMetrics.widthPixels / displayMetrics.density).toInt()
    return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
}
