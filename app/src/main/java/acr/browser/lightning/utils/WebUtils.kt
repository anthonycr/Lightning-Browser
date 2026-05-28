package acr.browser.lightning.utils

import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.database.history.HistoryRepository
import android.app.Application
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewDatabase
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WebUtils @Inject constructor(
    private val application: Application,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val historyRepository: HistoryRepository,
) {
    suspend fun clearCookies() = withContext(coroutineDispatchers.io) {
        CookieManager.getInstance().removeAllCookies(null)
    }

    suspend fun clearWebStorage() = withContext(coroutineDispatchers.io) {
        WebStorage.getInstance().deleteAllData()
    }

    suspend fun clearHistory() = withContext(coroutineDispatchers.io) {
        historyRepository.deleteHistory()
        val webViewDatabase = WebViewDatabase.getInstance(application)
        webViewDatabase.clearFormData()
        webViewDatabase.clearHttpAuthUsernamePassword()
        application.cacheDir.deleteRecursively()
    }

    suspend fun clearCache() = withContext(coroutineDispatchers.main) {
        val webView = WebView(application)
        webView.clearCache(true)
        webView.destroy()
        application.cacheDir.deleteRecursively()
    }
}
