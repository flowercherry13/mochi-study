package com.mochistudy.app

import android.app.Activity
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : Activity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(ScheduleBridge(), "MochiAndroid")
        setContentView(webView)
        webView.loadUrl("https://flowercherry13.github.io/mochi-study/")
    }

    inner class ScheduleBridge {
        @JavascriptInterface
        fun saveSchedule(json: String) {
            getSharedPreferences("mochi_widget", MODE_PRIVATE)
                .edit().putString("schedule_json", json).apply()
            runOnUiThread { ScheduleWidgetProvider.refreshAll(this@MainActivity) }
        }
    }
}
