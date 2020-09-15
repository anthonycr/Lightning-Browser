package acr.browser.lightning._browser2.tab

import acr.browser.lightning.ssl.SslState
import io.reactivex.Observable

/**
 * Created by anthonycr on 9/11/20.
 */
data class Tab(
    val id: Int,
    val icon: String,
    val title: String,
    val isSelected: Boolean
)

/*
probably needs to be an interface
 */


interface TabModel {

    val id: Int

    // Navigation

    fun loadUrl(url: String)

    fun goBack()

    fun canGoBack(): Boolean

    fun canGoBackChanges(): Observable<Boolean>

    fun goForward()

    fun canGoForward(): Boolean

    fun canGoForwardChanges(): Observable<Boolean>

    fun reload()

    fun stopLoading()

    // Data

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

}
