package org.wsb.mescidtv

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader

/**
 * MainActivity for WSB Hamburg Mescid TV
 * Specially engineered for 24/7 continuous operation on Android TV and Amazon Fire TV Stick.
 * Enforces multi-tier Window, View, Keyguard, and PowerManager screen keep-awake policies.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var wakeLock: PowerManager.WakeLock? = null

    // Serves the bundled web app from a secure https origin. Plain file:// URLs block the
    // ES-module script of the Vite build inside WebView (blank screen), so this is required.
    private val assetLoader: WebViewAssetLoader by lazy {
        WebViewAssetLoader.Builder()
            .setDomain("appassets.androidplatform.net")
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on permanently for Android TV / Fire TV Stick 24/7 continuous operation
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 1. Multi-Tier Screen Keep-Awake Strategy
        enforceKeepScreenOn()

        // 2. Hide system status & navigation bars for immersive 16:9 TV display
        hideSystemUI()

        // 3. Initialize WebView with keepScreenOn enabled on the view hierarchy
        webView = WebView(this).apply {
            layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            isFocusable = true
            isFocusableInTouchMode = true
            keepScreenOn = true // View-level signal to WindowManager
        }
        setContentView(webView)

        // Ensure decorView also holds keepScreenOn flag
        window.decorView.keepScreenOn = true

        configureWebSettings()

        // 4. Acquire CPU Partial WakeLock as background safety net for continuous network updates
        acquireCpuWakeLock()

        // 5. Load the bundled offline-first web application
        webView.loadUrl(APP_URL)
    }

    /**
     * Enforces WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON along with
     * lockscreen bypass and turn-screen-on flags across all Android API versions.
     */
    private fun enforceKeepScreenOn() {
        // Essential Window flag preventing screen sleep
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Ensure the screen stays on and unlocks automatically upon boot / power recovery
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    /**
     * Holds a Partial WakeLock to ensure system services, background timers,
     * and network synchronization continue 24/7 even under aggressive TV power policies.
     */
    @SuppressLint("WakelockTimeout")
    private fun acquireCpuWakeLock() {
        try {
            if (wakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = powerManager?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "WSBMescidTV:24_7_DisplayKeepAwake"
                )?.apply {
                    setReferenceCounted(false)
                }
            }
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire()
            }
        } catch (e: Exception) {
            // Non-critical fallback; WindowManager FLAG_KEEP_SCREEN_ON is primary
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebSettings() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccess = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest
            ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }

            // If the page could not be loaded (rare), retry automatically after 30 seconds
            @RequiresApi(Build.VERSION_CODES.M)
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                val v = view ?: return
                if (request?.isForMainFrame == true) {
                    v.postDelayed({ v.loadUrl(APP_URL) }, 30_000)
                }
            }

            // If the WebView renderer process dies (memory), rebuild the screen instead of crashing
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                runOnUiThread { recreate() }
                return true
            }
        }
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        }
    }

    override fun onResume() {
        super.onResume()
        AppState.visible = true
        AppState.userStopped = false
        AppState.lastVisibleAt = System.currentTimeMillis()
        KeepAliveService.start(this)
        hideSystemUI()
        enforceKeepScreenOn()
        webView.keepScreenOn = true
        window.decorView.keepScreenOn = true
        acquireCpuWakeLock()
    }

    override fun onPause() {
        AppState.visible = false
        AppState.lastVisibleAt = System.currentTimeMillis()
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
            enforceKeepScreenOn()
            webView.keepScreenOn = true
            window.decorView.keepScreenOn = true
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        enforceKeepScreenOn()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
    }

    // Handle TV Remote D-Pad and Menu hardware keys (Amazon Fire TV & Android TV)
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_MENU -> {
                    // Open Admin PIN dialog via JavaScript
                    webView.evaluateJavascript("window.dispatchEvent(new CustomEvent('tv-remote-menu'));", null)
                    return true
                }
                KeyEvent.KEYCODE_BACK -> {
                    // Hold BACK for 3 seconds to close the app on purpose (also stops the watchdog)
                    if (event.repeatCount > 0 && event.eventTime - event.downTime >= 3000) {
                        AppState.userStopped = true
                        stopService(android.content.Intent(this, KeepAliveService::class.java))
                        finishAndRemoveTask()
                        return true
                    }
                    // Short press: only tell the web page (closes open dialogs)
                    if (event.repeatCount == 0) {
                        webView.evaluateJavascript("window.dispatchEvent(new CustomEvent('tv-remote-back'));", null)
                    }
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    companion object {
        private const val APP_URL = "https://appassets.androidplatform.net/assets/www/index.html"
    }
}
