package acr.browser.lightning.browser.search

import acr.browser.lightning.R
import acr.browser.lightning.constant.HTTPS
import acr.browser.lightning.preference.UserPreferencesDataStore
import acr.browser.lightning.utils.isSpecialUrl
import android.app.Application
import android.webkit.URLUtil
import androidx.core.net.toUri
import dagger.Reusable
import javax.inject.Inject

/**
 * A UI model for the search box.
 */
@Reusable
class SearchBoxModel @Inject constructor(
    private val userPreferencesDataStore: UserPreferencesDataStore,
    application: Application
) {

    private val untitledTitle: String = application.getString(R.string.untitled)

    /**
     * Returns the contents of the search box based on a variety of factors.
     *
     *  - The user's preference to show either the URL, domain, or page title
     *  - Whether or not the current page is loading
     *  - Whether or not the current page is a Lightning generated page.
     *
     * This method uses the URL, title, and loading information to determine what
     * should be displayed by the search box.
     *
     * @param url       the URL of the current page.
     * @param title     the title of the current page, if known.
     * @param isLoading whether the page is currently loading or not.
     * @return the string that should be displayed by the search box.
     */
    suspend fun getDisplayContent(url: String, title: String?, isLoading: Boolean): String =
        when {
            url.isSpecialUrl() -> ""
            isLoading -> url
            else -> when (userPreferencesDataStore.urlBoxContentChoice.get()) {
                SearchBoxDisplayChoice.DOMAIN -> getDisplayDomainName(url)
                SearchBoxDisplayChoice.URL -> url
                SearchBoxDisplayChoice.TITLE ->
                    if (title?.isEmpty() == false) {
                        title
                    } else {
                        untitledTitle
                    }
            }
        }

    /**
     * Extracts the domain name from a URL.
     * NOTE: Should be used for display only.
     *
     * @param rawUrl the URL to extract the domain from.
     * @return the domain name, or the URL if the domain could not be extracted. The domain name
     * will be prefixed with https:// if the URL is an SSL supported URL.
     */
    fun getDisplayDomainName(rawUrl: String?): String {
        if (rawUrl.isNullOrEmpty()) return ""

        val ssl = URLUtil.isHttpsUrl(rawUrl)
        val index = rawUrl.indexOf('/', 8)
        val sanitizedUrl = if (index != -1) {
            rawUrl.substring(0, index)
        } else {
            rawUrl
        }

        val domain: String? = sanitizedUrl.toUri().host

        return if (domain.isNullOrEmpty()) {
            sanitizedUrl
        } else if (ssl) {
            HTTPS + domain
        } else if (domain.startsWith("www.")) {
            domain.substring(4)
        } else {
            domain
        }
    }
}
