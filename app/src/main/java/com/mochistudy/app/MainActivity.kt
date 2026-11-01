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
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                view?.evaluateJavascript(
                    """(function() {
                        function syncMochiSchedule() {
                            try {
                                var saved = JSON.parse(localStorage.getItem('mochiStudy') || '{}');
                                if (saved.lessons && window.MochiAndroid) {
                                    window.MochiAndroid.saveSchedule(JSON.stringify(saved.lessons));
                                }
                            } catch (e) {}
                        }
                        if (!Storage.prototype.__mochiStudyPatched) {
                            var originalSetItem = Storage.prototype.setItem;
                            Storage.prototype.setItem = function(key, value) {
                                originalSetItem.apply(this, arguments);
                                if (key === 'mochiStudy') syncMochiSchedule();
                            };
                            Storage.prototype.__mochiStudyPatched = true;
                        }
                        syncMochiSchedule();
                    })();""",
                    null
                )
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
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
        webView.addJavascriptInterface(ScheduleBridge(), "MochiAndroid")
        setContentView(webView)
        webView.loadUrl("https://flowercherry13.github.io/mochi-study/")
    }

    @Deprecated("Deprecated in Android, retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_CHOOSER_REQUEST) {
            val result = if (resultCode == RESULT_OK) WebChromeClient.FileChooserParams.parseResult(resultCode, data) else null
            filePathCallback?.onReceiveValue(result)
            filePathCallback = null
        }
    }

    inner class ScheduleBridge {
        @JavascriptInterface
        fun saveSchedule(json: String) {
            getSharedPreferences("mochi_widget", MODE_PRIVATE)
                .edit().putString("schedule_json", json).apply()
            runOnUiThread { ScheduleWidgetProvider.refreshAll(this@MainActivity) }
        }
    }

    companion object {
        private const val FILE_CHOOSER_REQUEST = 2401
    }
}
