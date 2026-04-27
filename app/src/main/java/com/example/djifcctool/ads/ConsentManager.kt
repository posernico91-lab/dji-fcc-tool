package com.example.djifcctool.ads

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.example.djifcctool.BuildConfig
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Kapselt die Google User Messaging Platform (UMP / IAB TCF v2)
 * für DSGVO-konformes Consent-Handling vor dem Laden von Werbung.
 *
 * Ablauf:
 *   1. requestConsent() — UMP fragt den Server, ob ein Consent-Form
 *      gezeigt werden muss (Region-abhängig; in EU/EWR: ja).
 *   2. Falls Form verfügbar → Activity-Dialog wird geladen + ggf. gezeigt.
 *   3. Sobald Consent verfügbar ist (oder nicht erforderlich), wird das
 *      onReady-Callback gefeuert.
 *
 * NPA-Logik: Wenn der Nutzer personalisierte Werbung NICHT erlaubt,
 * liefert [buildAdRequest] automatisch ein extras-Bundle mit `npa=1`,
 * sodass alle Ad-Loads non-personalized abgesetzt werden.
 */
object ConsentManager {
    private const val TAG = "ConsentManager"
    private const val PREFS = "fcc_switch_consent"
    private const val KEY_BUILTIN_DECISION = "builtin_decision" // "accept_personalized", "accept_npa", "reject", null
    private const val KEY_BUILTIN_TIMESTAMP = "builtin_ts"

    @Volatile private var consentInformation: ConsentInformation? = null
    private val initialized = AtomicBoolean(false)

    // ---------- Built-in Consent (immer verfügbar, unabhängig von AdMob Console) ----------

    enum class BuiltinDecision { NONE, ACCEPT_PERSONALIZED, ACCEPT_NPA, REJECT }

    fun getBuiltinDecision(context: Context): BuiltinDecision {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return when (prefs.getString(KEY_BUILTIN_DECISION, null)) {
            "accept_personalized" -> BuiltinDecision.ACCEPT_PERSONALIZED
            "accept_npa" -> BuiltinDecision.ACCEPT_NPA
            "reject" -> BuiltinDecision.REJECT
            else -> BuiltinDecision.NONE
        }
    }

    fun setBuiltinDecision(context: Context, decision: BuiltinDecision) {
        val value = when (decision) {
            BuiltinDecision.ACCEPT_PERSONALIZED -> "accept_personalized"
            BuiltinDecision.ACCEPT_NPA -> "accept_npa"
            BuiltinDecision.REJECT -> "reject"
            BuiltinDecision.NONE -> null
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_BUILTIN_DECISION, value)
            .putLong(KEY_BUILTIN_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    fun resetBuiltinDecision(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    /**
     * Lädt + zeigt das Consent-Form (falls erforderlich).
     * onReady wird aufgerufen, sobald die App Ads laden darf.
     */
    fun requestConsent(activity: Activity, onReady: (canRequestAds: Boolean) -> Unit) {
        val paramsBuilder = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
        if (BuildConfig.DEBUG) {
            // Im Debug-Build: Geografie auf EEA forcen, damit der
            // Consent-Dialog auch auf Test-Ger\u00e4ten au\u00dferhalb der EU
            // verl\u00e4sslich erscheint.
            val debug = ConsentDebugSettings.Builder(activity)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .addTestDeviceHashedId("TEST-DEVICE-HASHED-ID")
                .build()
            paramsBuilder.setConsentDebugSettings(debug)
        }
        val params = paramsBuilder.build()

        val info = UserMessagingPlatform.getConsentInformation(activity).also {
            consentInformation = it
        }
        info.requestConsentInfoUpdate(
            activity, params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form error: ${formError.message}")
                    }
                    onReady(canRequestAds(activity))
                }
            },
            { requestError ->
                Log.w(TAG, "Consent request error: ${requestError.message}")
                onReady(canRequestAds(activity))
            }
        )
    }

    /**
     * Erzwingt das erneute Anzeigen des Consent-Forms (auch wenn der
     * Nutzer bereits zugestimmt hat). N\u00fctzlich, wenn der Nutzer
     * \u00fcber den Datenschutz-Button die Auswahl \u00e4ndern m\u00f6chte
     * und [isPrivacyOptionsRequired] aktuell false zur\u00fcckgibt.
     */
    fun forceShowConsentForm(activity: Activity) {
        UserMessagingPlatform.loadConsentForm(activity, { form ->
            form.show(activity) { error ->
                if (error != null) Log.w(TAG, "force consent form error: ${error.message}")
            }
        }, { error ->
            Log.w(TAG, "loadConsentForm error: ${error.message}")
        })
    }

    /**
     * Setzt den UMP-Status komplett zur\u00fcck (Debug-Helfer) und
     * fordert den Consent erneut an.
     */
    fun resetAndReshow(activity: Activity) {
        consentInformation?.reset()
            ?: UserMessagingPlatform.getConsentInformation(activity).also {
                consentInformation = it
                it.reset()
            }
        requestConsent(activity) { /* no-op */ }
    }

    /** Privacy-Options-Form: über Einstellungs-Eintrag erreichbar. */
    fun isPrivacyOptionsRequired(): Boolean =
        consentInformation?.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptionsForm(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            if (error != null) Log.w(TAG, "Privacy form error: ${error.message}")
        }
    }

    /** True, sobald wir grünes Licht für Ad-Requests haben (Built-in ODER UMP). */
    fun canRequestAds(context: Context): Boolean {
        // Priorität: eigener Built-in-Consent. Wenn Nutzer "REJECT" gewählt hat → keine Ads.
        when (getBuiltinDecision(context)) {
            BuiltinDecision.ACCEPT_PERSONALIZED, BuiltinDecision.ACCEPT_NPA -> return true
            BuiltinDecision.REJECT -> return false
            BuiltinDecision.NONE -> { /* falle durch zu UMP */ }
        }
        val info = consentInformation
            ?: UserMessagingPlatform.getConsentInformation(context).also { consentInformation = it }
        return info.canRequestAds()
    }

    /**
     * NPA aktiv, wenn:
     *  – Built-in: Nutzer hat ACCEPT_NPA oder REJECT gewählt, ODER
     *  – Built-in: keine Entscheidung UND TCF sagt Purpose 1 != '1'.
     */
    fun isNonPersonalized(context: Context): Boolean {
        when (getBuiltinDecision(context)) {
            BuiltinDecision.ACCEPT_PERSONALIZED -> return false
            BuiltinDecision.ACCEPT_NPA, BuiltinDecision.REJECT -> return true
            BuiltinDecision.NONE -> { /* falle durch zu TCF-Check */ }
        }
        val prefs = context.getSharedPreferences(
            context.packageName + "_preferences", Context.MODE_PRIVATE
        )
        val tcfPurposeConsents = prefs.getString("IABTCF_PurposeConsents", null)
        if (tcfPurposeConsents.isNullOrEmpty()) return true
        val purpose1 = tcfPurposeConsents.firstOrNull() ?: return true
        return purpose1 != '1'
    }

    /** Baut einen AdRequest mit NPA-Extras, falls notwendig. */
    fun buildAdRequest(context: Context): AdRequest {
        val builder = AdRequest.Builder()
        if (isNonPersonalized(context)) {
            val extras = Bundle().apply { putString("npa", "1") }
            builder.addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
        }
        return builder.build()
    }

    fun markInitialized() = initialized.set(true)
    fun isInitialized(): Boolean = initialized.get()
}
