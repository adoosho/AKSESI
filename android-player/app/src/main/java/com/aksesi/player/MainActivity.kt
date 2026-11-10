package com.aksesi.player

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.VideoView

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var root: FrameLayout
    private val nativeVideos = HashMap<Int, VideoView>()
    private val nativeVideoUrls = HashMap<Int, String>()

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

    private inner class AksesiMedia {
        @JavascriptInterface
        fun play(zone: Int, url: String, left: Float, top: Float, width: Float, height: Float) {
            if (zone !in 0..3 || !isAllowedMediaUrl(url)) return
            if (width <= 0f || height <= 0f || width > 5000f || height > 5000f) return
            runOnUiThread { showNativeVideo(zone, url, left, top, width, height) }
        }

        @JavascriptInterface
        fun stop(zone: Int) {
            if (zone !in 0..3) return
            runOnUiThread { removeNativeVideo(zone) }
        }
    }

    private fun isAllowedMediaUrl(url: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            uri.scheme == "https" &&
                uri.host == "bnrogtqhxbqinrtzwblv.supabase.co" &&
                (uri.path ?: "").contains("/storage/v1/object/")
        } catch (_: Exception) {
            false
        }
    }

    private fun showNativeVideo(zone: Int, url: String, left: Float, top: Float, width: Float, height: Float) {
        val density = resources.displayMetrics.density
        val video = nativeVideos[zone] ?: VideoView(this).also { view ->
            view.setBackgroundColor(Color.BLACK)
            view.setOnPreparedListener { player: MediaPlayer ->
                player.setVolume(0f, 0f)
                player.isLooping = true
                view.start()
                Log.i("AKSESI_VIDEO", "VIDEO_PREPARED zone=$zone duration=${player.duration}")
                view.postDelayed({
                    val advancing = try { view.isPlaying && view.currentPosition > 0 } catch (_: Exception) { false }
                    Log.i("AKSESI_VIDEO", if (advancing) "VIDEO_PLAYING zone=$zone position=${view.currentPosition}" else "VIDEO_NOT_ADVANCING zone=$zone")
                }, 1500)
            }
            view.setOnErrorListener { _, what, extra ->
                Log.e("AKSESI_VIDEO", "VIDEO_ERROR zone=$zone what=$what extra=$extra")
                removeNativeVideo(zone)
                true
            }
            nativeVideos[zone] = view
            root.addView(view)
        }

        val params = (video.layoutParams as? FrameLayout.LayoutParams)
            ?: FrameLayout.LayoutParams(1, 1)
        params.width = (width * density).toInt().coerceAtLeast(1)
        params.height = (height * density).toInt().coerceAtLeast(1)
        params.gravity = Gravity.TOP or Gravity.LEFT
        params.leftMargin = (left * density).toInt()
        params.topMargin = (top * density).toInt()
        video.layoutParams = params

        if (nativeVideoUrls[zone] != url) {
            nativeVideoUrls[zone] = url
            video.visibility = View.VISIBLE
            video.setVideoURI(Uri.parse(url))
            video.requestFocus()
        } else if (video.visibility != View.VISIBLE) {
            video.visibility = View.VISIBLE
        }
    }

    private fun removeNativeVideo(zone: Int) {
        val video = nativeVideos.remove(zone) ?: return
        nativeVideoUrls.remove(zone)
        try {
            video.stopPlayback()
        } catch (_: Exception) {
        }
        root.removeView(video)
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
        webView.addJavascriptInterface(AksesiMedia(), "AksesiMedia")
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return handleNavigation(url)
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
        }

        root.addView(
            webView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        setContentView(root)
        try {
            val html = assets.open("player-legacy.html").bufferedReader(Charsets.UTF_8).use { it.readText() }
            webView.loadDataWithBaseURL(
                "https://www.aksesi.space/",
                html,
                "text/html",
                "UTF-8",
                null
            )
        } catch (_: Exception) {
            showError()
        }

        // Debug-only API 22 playback test hook. The CI workflow supplies a short
        // H.264/AAC file on the emulator; production builds ignore this extra.
        if (BuildConfig.DEBUG && intent.action == "com.aksesi.player.TEST_VIDEO") {
            val testPath = intent.getStringExtra("aksesi_test_video")
            if (!testPath.isNullOrBlank() && testPath.startsWith("/sdcard/")) {
                root.postDelayed({
                    val density = resources.displayMetrics.density
                    showNativeVideo(
                        0,
                        Uri.fromFile(java.io.File(testPath)).toString(),
                        0f,
                        0f,
                        root.width / density,
                        root.height / density
                    )
                }, 2500)
            }
        }
    }

    private fun showError() {
        runOnUiThread {
            nativeVideos.keys.toList().forEach { removeNativeVideo(it) }
            val message = TextView(this).apply {
                text = "Nuk mund të hapet AKSESI. Kontrollo internetin dhe prek ekranin për të provuar përsëri."
                setTextColor(Color.WHITE)
                textSize = 20f
                gravity = Gravity.CENTER
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
        nativeVideos.keys.toList().forEach { removeNativeVideo(it) }
        webView.destroy()
        super.onDestroy()
    }
}
