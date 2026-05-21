package acr.browser.lightning.utils

import acr.browser.lightning.database.history.HistoryRepository
import android.app.Activity
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object WebUtils {
    fun clearCookies() {
        CookieManager.getInstance().removeAllCookies(null)
    }

    fun clearWebStorage() {
        WebStorage.getInstance().deleteAllData()
    }

    fun clearHistory(
        context: Context,
        historyRepository: HistoryRepository,
        coroutineScope: CoroutineScope
    ) {
        coroutineScope.launch {
            historyRepository.deleteHistory()
        }
        val webViewDatabase = WebViewDatabase.getInstance(context)
        webViewDatabase.clearFormData()
        webViewDatabase.clearHttpAuthUsernamePassword()
        Utils.trimCache(context)
    }

    fun clearCache(activity: Activity) {
        val webView = WebView(activity)
        webView.clearCache(true)
        webView.destroy()
    }
}
