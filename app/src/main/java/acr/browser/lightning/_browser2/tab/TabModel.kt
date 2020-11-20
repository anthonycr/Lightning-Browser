package acr.browser.lightning._browser2.tab

import acr.browser.lightning.ssl.SslState
import acr.browser.lightning.view.TabInitializer
import android.graphics.Bitmap
import android.os.Bundle
import io.reactivex.Observable

interface TabModel {

    val id: Int

    // Navigation

    fun loadUrl(url: String)

    fun loadFromInitializer(tabInitializer: TabInitializer)

    fun goBack()

    fun canGoBack(): Boolean

    fun canGoBackChanges(): Observable<Boolean>

    fun goForward()

    fun canGoForward(): Boolean

    fun canGoForwardChanges(): Observable<Boolean>

    fun reload()

    fun stopLoading()

    fun find(query: String)

    fun findNext()

    fun findPrevious()

    fun clearFindMatches()

    val findQuery: String?

    // Data

    val favicon: Bitmap?

    fun faviconChanges(): Observable<Bitmap>

    val url: String

    fun urlChanges(): Observable<String>

    val title: String

    fun titleChanges(): Observable<String>

    val sslState: SslState

    fun sslChanges(): Observable<SslState>

    val loadingProgress: Int

    fun loadingProgress(): Observable<Int>

    // Lifecycle

    var isForeground: Boolean

    fun destroy()

    fun freeze(): Bundle

}
