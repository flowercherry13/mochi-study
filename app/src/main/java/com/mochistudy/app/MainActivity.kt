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
import org.json.JSONArray

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
                // The page can still be initializing its schedule; retry after it settles.
                listOf(0L, 500L, 1500L, 3000L, 6000L).forEach { delay ->
                    webView.postDelayed({ syncScheduleFromWeb() }, delay)
                }
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
                if (!window.MochiAndroid) return 'bridge-missing';
                var scheduleData = null;
                var raw = window.localStorage.getItem('mochiStudy');
                if (raw) scheduleData = JSON.parse(raw);
                // The app's top-level `let data` is not necessarily exposed as window.data.
                if (!scheduleData && typeof data !== 'undefined') scheduleData = data;
                if (!scheduleData && window.data) scheduleData = window.data;
                var lessons = Array.isArray(scheduleData) ? scheduleData : (scheduleData && scheduleData.lessons);
                if (!Array.isArray(lessons)) return raw ? 'no-lessons-in-storage' : 'no-schedule-data-yet';
                if (!raw) {
                  try { window.localStorage.setItem('mochiStudy', JSON.stringify(scheduleData)); } catch (ignored) {}
                }
                window.MochiAndroid.saveSchedule(JSON.stringify(lessons));
                return 'sent:' + lessons.length;
              } catch (e) { return 'sync-error:' + String(e); }
            })();""",
            { result ->
                val status = result?.trim('"') ?: "no-result"
                getSharedPreferences("mochi_widget", MODE_PRIVATE).edit()
                    .putString("sync_status", status)
                    .putLong("sync_checked_at", System.currentTimeMillis())
                    .apply()
                ScheduleWidgetProvider.refreshAll(this)
            }
        )
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) {
            listOf(300L, 1200L, 3000L).forEach { delay ->
                webView.postDelayed({ syncScheduleFromWeb() }, delay)
            }
        }
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
            try {
                val lessons = JSONArray(json)
                getSharedPreferences("mochi_widget", MODE_PRIVATE).edit()
                    .putString("schedule_json", lessons.toString())
                    .putString("sync_status", "saved:" + lessons.length())
                    .putLong("sync_saved_at", System.currentTimeMillis())
                    .apply()
                runOnUiThread { ScheduleWidgetProvider.refreshAll(this@MainActivity) }
            } catch (e: Exception) {
                getSharedPreferences("mochi_widget", MODE_PRIVATE).edit()
                    .putString("sync_status", "invalid-json:" + (e.message ?: "parse error"))
                    .apply()
            }
        }
    }

    companion object {
        private const val FILE_CHOOSER_REQUEST = 2401
    }
}
