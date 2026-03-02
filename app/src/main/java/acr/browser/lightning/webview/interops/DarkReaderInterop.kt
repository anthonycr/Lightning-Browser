package acr.browser.lightning.webview.interops

import acr.browser.lightning.AppTheme
import acr.browser.lightning.js.DarkReader
import acr.browser.lightning.preference.UserPreferences
import android.webkit.JavascriptInterface
import android.webkit.WebView
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class DarkReaderInterop @Inject constructor(
    private val darkReader: DarkReader,
    private val userPreferences: UserPreferences
) : InteropInterface {

    override fun register(webView: WebView) {
        webView.addJavascriptInterface(this, HANDLER)
    }

    override fun onPageFinished(webView: WebView, url: String) {
        if (userPreferences.useTheme == AppTheme.DARK || userPreferences.useTheme == AppTheme.BLACK) {
            startService(webView)
        }
    }

    private fun startService(view: WebView) {
        view.evaluateJavascript(darkReader.provideJs()) {
            view.evaluateJavascript(
                """
                DarkReader.setFetchMethod(function(url) {
                    return new Promise(function(resolve, reject) {
                        try {
                            let cssText = window.$HANDLER.fetchCss(url);
                            resolve(new Response(cssText, {status: 200, statusText: "OK"}));
                        } catch (e) {
                            reject(e);
                        }
                    });
                });
                DarkReader.enable();
                """, null)
        }
    }

    @Suppress("unused")
    @JavascriptInterface
    fun fetchCss(url: String): String {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            ""
        }
    }

    companion object {
        private const val HANDLER = "DarkReaderHandler"
    }
}