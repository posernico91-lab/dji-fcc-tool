package com.example.djifcctool

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.djifcctool.ads.AdsManager
import com.example.djifcctool.ads.ConsentManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Einzige Activity der App. Kümmert sich nur um Boilerplate:
 *   – Hilt-Bootstrap
 *   – USB-Permission-BroadcastReceiver
 *   – Compose-Root-Setup
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private companion object {
        const val ACTION_USB_PERMISSION = "com.example.djifcctool.USB_PERMISSION"
    }

    private val viewModel: MainViewModel by viewModels()
    private lateinit var permissionIntent: PendingIntent
    private lateinit var systemUsbManager: UsbManager

    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_USB_PERMISSION) return
            val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            device?.let { viewModel.onUsbPermissionResult(it, granted) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        systemUsbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
        permissionIntent = PendingIntent.getBroadcast(
            this, 0, Intent(ACTION_USB_PERMISSION).setPackage(packageName), flags
        )

        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbPermissionReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(usbPermissionReceiver, filter)
        }

        lifecycleScope.launch {
            viewModel.events.collect { event ->
                when (event) {
                    is UiEvent.RequestUsbPermission ->
                        systemUsbManager.requestPermission(event.device, permissionIntent)
                }
            }
        }

        setContent {
            DjiFccToolTheme {
                MainScreen(viewModel)
            }
        }

        // UMP / GDPR Consent VOR jeglicher Ad-Initialisierung.
        ConsentManager.requestConsent(this) { canRequestAds ->
            if (canRequestAds) {
                (application as DjiFccApp).onConsentReady()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AdsManager.onActivityResumed(this)
    }

    override fun onPause() {
        AdsManager.onActivityPaused(this)
        super.onPause()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(usbPermissionReceiver) }
        super.onDestroy()
    }
}
