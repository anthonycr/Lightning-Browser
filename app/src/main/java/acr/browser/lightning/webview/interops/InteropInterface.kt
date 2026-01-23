package acr.browser.lightning.webview.interops

import android.webkit.WebView

interface InteropInterface {

    /**
     * Called to register the JS interface with the [WebView].
     */
    fun register(webView: WebView)

    /**
     * Called when a page has finished loading in the [WebView].
     */
    fun onPageFinished(webView: WebView, url: String)

}
