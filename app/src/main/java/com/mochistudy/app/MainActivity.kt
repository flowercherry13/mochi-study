package com.mochistudy.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.addJavascriptInterface(ScheduleBridge(), "MochiAndroid")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                syncScheduleFromWeb()
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(view: WebView?, callback: ValueCallback<Array<Uri>>?, params: FileChooserParams?): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = callback
                val intent = params?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                }
                return try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST)
                    true
                } catch (_: Exception) {
                    filePathCallback = null
                    false
                }
            }
        }
        setContentView(webView)
        webView.loadUrl("https://flowercherry13.github.io/mochi-study/")
    }

    private fun syncScheduleFromWeb() {
        if (!::webView.isInitialized) return
        webView.evaluateJavascript(
            """(function() {
              try {
                var raw = window.localStorage.getItem('mochiStudy');
                if (!raw || !window.MochiAndroid) return 'no-data-or-bridge';
                var data = JSON.parse(raw);
                var lessons = Array.isArray(data) ? data : data.lessons;
                if (!Array.isArray(lessons)) return 'no-lessons';
                window.MochiAndroid.saveSchedule(JSON.stringify(lessons));
                return 'synced:' + lessons.length;
              } catch (e) { return 'sync-error:' + e; }
            })();""",
            null
        )
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) webView.postDelayed({ syncScheduleFromWeb() }, 700)
    }

    @Deprecated("Deprecated in Android, retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_CHOOSER_REQUEST) {
            val result = if (resultCode == RESULT_OK) WebChromeClient.FileChooserParams.parseResult(resultCode, data) else null
            filePathCallback?.onReceiveValue(result)
            filePathCallback = null
            syncScheduleFromWeb()
        }
    }

    inner class ScheduleBridge {
        @JavascriptInterface
        fun saveSchedule(json: String) {
            getSharedPreferences("mochi_widget", MODE_PRIVATE).edit().putString("schedule_json", json).apply()
            runOnUiThread { ScheduleWidgetProvider.refreshAll(this@MainActivity) }
        }
    }

    companion object {
        private const val FILE_CHOOSER_REQUEST = 2401
    }
}
