package acr.browser.lightning.html

/**
 * A factory that builds an HTML page.
 */
interface HtmlPageFactory {

    /**
     * Build the HTML page and emit the URL.
     */
    suspend fun buildPage(): String

}
