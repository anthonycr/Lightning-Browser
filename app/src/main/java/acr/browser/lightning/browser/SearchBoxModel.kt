package acr.browser.lightning.browser

import acr.browser.lightning.BrowserApp
import acr.browser.lightning.R
import acr.browser.lightning.preference.PreferenceManager
import acr.browser.lightning.utils.UrlUtils
import acr.browser.lightning.utils.Utils
import android.app.Application
import javax.inject.Inject

/**
 * A UI model for the search box.
 */
class SearchBoxModel @Inject constructor() {

    @Inject internal lateinit var mPreferences: PreferenceManager
    @Inject internal lateinit var mApplication: Application

    private val mUntitledTitle: String

    init {
        BrowserApp.getAppComponent().inject(this)
        mUntitledTitle = mApplication.getString(R.string.untitled)
    }

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
     *
     * @param title     the title of the current page, if known.
     *
     * @param isLoading whether the page is currently loading or not.
     *
     * @return the string that should be displayed by the search box.
     */
    fun getDisplayContent(url: String, title: String?, isLoading: Boolean): String {
        if (UrlUtils.isSpecialUrl(url)) {
            return ""
        } else if (isLoading) {
            return url
        } else {
            when (mPreferences.urlBoxContentChoice) {
                1 -> {
                    // URL, show the entire URL
                    return url
                }
                2 -> {
                    // Title, show the page's title
                    if (title?.isEmpty() == false) {
                        return title
                    } else {
                        return mUntitledTitle
                    }
                }
                0 -> {
                    // Default, show only the domain
                    return safeDomain(url)
                }
                else -> {
                    // Default, show only the domain
                    return safeDomain(url)
                }
            }
        }
    }

    private fun safeDomain(url: String): String {
        return Utils.getDomainName(url) ?: url
    }

}
