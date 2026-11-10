package com.aksesi.player

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var root: FrameLayout

    private class AksesiStore(context: Context) {
        private val prefs = context.getSharedPreferences("aksesi_player_secure", Context.MODE_PRIVATE)
        private val allowed = setOf("sig_pcode", "sig_ptoken", "sig_ptoken_code")

        @JavascriptInterface
        fun get(key: String): String {
            if (key !in allowed) return ""
            return prefs.getString(key, "") ?: ""
        }

        @JavascriptInterface
        fun set(key: String, value: String) {
            if (key !in allowed || value.length > 512) return
            if ((key == "sig_pcode" || key == "sig_ptoken_code") &&
                !value.matches(Regex("[0-9]{6}"))) return
            prefs.edit().putString(key, value).apply()
        }

        @JavascriptInterface
        fun remove(key: String) {
            if (key in allowed) prefs.edit().remove(key).apply()
        }
    }

    @Suppress("DEPRECATION")
    private fun enterImmersiveMode() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
    }

    private fun isAksesiUrl(url: String): Boolean {
        val uri = Uri.parse(url)
        val host = uri.host ?: return false
        return uri.scheme == "https" && (host == "aksesi.space" || host.endsWith(".aksesi.space"))
    }

    private fun handleNavigation(url: String): Boolean {
        if (isAksesiUrl(url)) return false
        if (url.startsWith("https://")) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (_: Exception) {
                // Keep unsupported external links inside the player.
            }
        }
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        enterImmersiveMode()

        root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        webView = WebView(this)
        webView.setBackgroundColor(Color.BLACK)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.databaseEnabled = true
        webView.settings.loadsImagesAutomatically = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.settings.allowFileAccess = false
        webView.settings.allowContentAccess = false
        webView.addJavascriptInterface(AksesiStore(this), "AksesiStore")
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            // Android 5.1 and earlier use the String overload.
            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return handleNavigation(url)
            }

            // Android 6.0+ uses the WebResourceRequest overload.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return handleNavigation(request.url.toString())
            }

            @Suppress("DEPRECATION")
            override fun onReceivedError(
                view: WebView,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                val failed = failingUrl ?: return
                if (failed.contains("/player-legacy.html")) showError()
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) showError()
            }
        }

        root.addView(
            webView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        setContentView(root)
        webView.loadUrl("https://aksesi.space/player-legacy.html")
    }

    private fun showError() {
        runOnUiThread {
            val message = TextView(this).apply {
                text = "Nuk mund të hapet AKSESI. Kontrollo internetin dhe prek ekranin për të provuar përsëri."
                setTextColor(Color.WHITE)
                textSize = 20f
                gravity = android.view.Gravity.CENTER
                setPadding(40, 40, 40, 40)
                setBackgroundColor(Color.BLACK)
            }
            root.removeAllViews()
            root.addView(
                message,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
            root.setOnClickListener {
                root.setOnClickListener(null)
                root.removeAllViews()
                root.addView(
                    webView,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
                webView.reload()
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersiveMode()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else webView.reload()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
