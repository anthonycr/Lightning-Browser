package acr.browser.lightning.browser.tab.bundle

import acr.browser.lightning.R
import acr.browser.lightning.browser.tab.BookmarkPageInitializer
import acr.browser.lightning.browser.tab.BundleInitializer
import acr.browser.lightning.browser.tab.DownloadPageInitializer
import acr.browser.lightning.browser.tab.FreezableInitializer
import acr.browser.lightning.browser.tab.HistoryPageInitializer
import acr.browser.lightning.browser.tab.HomePageInitializer
import acr.browser.lightning.browser.tab.TabInitializer
import acr.browser.lightning.browser.tab.TabModel
import acr.browser.lightning.browser.tab.bundle.storage.BundleWriter
import acr.browser.lightning.utils.isBookmarkUrl
import acr.browser.lightning.utils.isDownloadsUrl
import acr.browser.lightning.utils.isHistoryUrl
import acr.browser.lightning.utils.isSpecialUrl
import acr.browser.lightning.utils.isStartPageUrl
import android.app.Application
import android.os.Bundle
import javax.inject.Inject

/**
 * A bundle store that serializes each tab state to disk and supports its retrieval.
 */
class DefaultBundleStore @Inject constructor(
    private val application: Application,
    private val bookmarkPageInitializer: BookmarkPageInitializer,
    private val homePageInitializer: HomePageInitializer,
    private val downloadPageInitializer: DownloadPageInitializer,
    private val historyPageInitializer: HistoryPageInitializer,
    bundleWriterFactory: BundleWriter.Factory,
) : BundleStore {

    private val bundleWriter = bundleWriterFactory.create(BUNDLE_STORAGE)

    override suspend fun save(tabs: List<TabModel>) {
        val outState = Bundle(ClassLoader.getSystemClassLoader())

        tabs.withIndex().forEach { (index, tab) ->
            if (!tab.url.isSpecialUrl()) {
                outState.putBundle(BUNDLE_KEY + index, tab.freeze())
            } else {
                outState.putBundle(BUNDLE_KEY + index, Bundle().apply {
                    putString(URL_KEY, tab.url)
                })
            }
            outState.putString(TAB_TITLE_KEY + index, tab.title)
            outState.putInt(TAB_ID_KEY + index, tab.id)
        }

        bundleWriter.writeToStorage(outState)
    }

    override suspend fun retrieve(): List<TabInitializer> =
        bundleWriter.readFromStorage()?.let { bundle ->
            bundle.keySet()
                .filter { it.startsWith(BUNDLE_KEY) }
                .mapNotNull { bundleKey ->
                    bundle.getBundle(bundleKey)?.let {
                        Triple(
                            it,
                            bundle.getString(TAB_TITLE_KEY + bundleKey.extractNumberFromEnd()),
                            bundle.getInt(TAB_ID_KEY + bundleKey.extractNumberFromEnd(), -1)
                        )
                    }
                }
        }?.map { (bundle, title, id) ->
            val delegate = bundle.getString(URL_KEY)?.let { url ->
                when {
                    url.isBookmarkUrl() -> bookmarkPageInitializer
                    url.isDownloadsUrl() -> downloadPageInitializer
                    url.isStartPageUrl() -> homePageInitializer
                    url.isHistoryUrl() -> historyPageInitializer
                    else -> homePageInitializer
                }
            } ?: BundleInitializer(bundle)

            FreezableInitializer(
                bundle = bundle,
                delegate = delegate,
                initialTitle = title ?: application.getString(R.string.tab_frozen),
                id = id
            )
        } ?: emptyList()

    override suspend fun deleteAll() {
        bundleWriter.deleteInStorage()
    }

    private fun String.extractNumberFromEnd(): String {
        val underScore = lastIndexOf('_')
        return if (underScore in indices) {
            substring(underScore + 1)
        } else {
            ""
        }
    }

    companion object {
        private const val BUNDLE_KEY = "WEBVIEW_"
        private const val TAB_TITLE_KEY = "TITLE_"
        private const val TAB_ID_KEY = "ID_"
        private const val URL_KEY = "URL_KEY"
        private const val BUNDLE_STORAGE = "SAVED_TABS.parcel"
    }
}
