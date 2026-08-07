package acr.browser.lightning.tab.initializer

/**
 * An initializer that is run on a [android.webkit.WebView] after it is created.
 *
 * @param T The underlying type of tab this initializer supports.
 */
interface TabInitializer<in T> {

    /**
     * Initialize the [android.webkit.WebView] instance held by the tab. If a url is loaded, the
     * provided [headers] should be used to load the url.
     */
    suspend fun initialize(tab: T, headers: Map<String, String>)

}
