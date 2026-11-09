package com.aksesi.player

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.JavascriptInterface
import android.content.Context
import android.widget.FrameLayout
import android.widget.TextView
import android.content.Intent
import android.net.Uri

class MainActivity : Activity() {
    private lateinit var webView: WebView

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
            if ((key == "sig_pcode" || key == "sig_ptoken_code") && !value.matches(Regex("[0-9]{6}"))) return
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
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        enterImmersiveMode()

        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        webView = WebView(this)
        webView.setBackgroundColor(Color.BLACK)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.databaseEnabled = true
        webView.settings.loadsImagesAutomatically = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.addJavascriptInterface(AksesiStore(this), "AksesiStore")
        webView.settings.allowFileAccess = false
        webView.settings.allowContentAccess = false
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                val host = uri.host ?: return true
                if (host == "aksesi.space" || host.endsWith(".aksesi.space")) return false
                if (uri.scheme == "https") {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                    return true
                }
                return true
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: android.webkit.WebResourceError
            ) {
                if (request.isForMainFrame) showError(root)
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
        webView.loadUrl("https://aksesi.space/player.html")
    }

    private fun showError(root: FrameLayout) {
        val message = TextView(this).apply {
            text = "Nuk ka lidhje me internetin. Kontrollo rrjetin dhe provo përsëri."
            setTextColor(Color.WHITE)
            textSize = 22f
            gravity = android.view.Gravity.CENTER
            setPadding(40, 40, 40, 40)
        }
        root.removeAllViews()
        root.addView(message)
        root.setOnClickListener {
            root.removeAllViews()
            root.addView(webView)
            webView.reload()
        }
    }

    @Suppress("DEPRECATION")
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersiveMode()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else webView.reload()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
