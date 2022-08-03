package acr.browser.lightning._browser2

import acr.browser.lightning._browser2.download.PendingDownload
import acr.browser.lightning._browser2.tab.TabModel
import acr.browser.lightning._browser2.tab.TabViewState
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.HistoryEntry
import acr.browser.lightning.database.downloads.DownloadEntry
import acr.browser.lightning.ssl.SslCertificateInfo
import acr.browser.lightning.view.TabInitializer
import android.content.Intent
import android.graphics.Bitmap
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import targetUrl.LongPress

/**
 * Created by anthonycr on 9/11/20.
 */


interface BrowserContract {

    interface View {

        fun renderState(viewState: BrowserViewState)

        fun renderTabs(tabs: List<TabViewState>)

        fun showAddBookmarkDialog(title: String, url: String, folders: List<String>)

        fun showBookmarkOptionsDialog(bookmark: Bookmark.Entry)

        fun showEditBookmarkDialog(
            title: String,
            url: String,
            folder: String,
            folders: List<String>
        )

        fun showFolderOptionsDialog(folder: Bookmark.Folder)

        fun showEditFolderDialog(title: String)

        fun showDownloadOptionsDialog(download: DownloadEntry)

        fun showHistoryOptionsDialog(historyEntry: HistoryEntry)

        fun showFindInPageDialog()

        fun showLinkLongPressDialog(longPress: LongPress)

        fun showImageLongPressDialog(longPress: LongPress)

        fun showSslDialog(sslCertificateInfo: SslCertificateInfo)

        fun showCloseBrowserDialog(id: Int)

        fun openBookmarkDrawer()

        fun closeBookmarkDrawer()

        fun openTabDrawer()

        fun closeTabDrawer()

        fun showToolbar()

        fun showToolsDialog(areAdsAllowed: Boolean, shouldShowAdBlockOption: Boolean)

        fun showLocalFileBlockedDialog()

        fun showFileChooser(intent: Intent)
    }

    interface Model {

        fun deleteTab(id: Int): Completable

        fun deleteAllTabs(): Completable

        fun createTab(tabInitializer: TabInitializer): Single<TabModel>

        fun reopenTab(): Maybe<TabModel>

        fun selectTab(id: Int): TabModel

        fun initializeTabs(): Maybe<List<TabModel>>

        fun freeze()

        fun clean()

        val tabsList: List<TabModel>

        fun tabsListChanges(): Observable<List<TabModel>>

    }

    interface Navigator {

        fun openSettings()

        fun openReaderMode(url: String)

        fun sharePage(url: String, title: String?)

        fun copyPageLink(url: String)

        fun closeBrowser()

        fun addToHomeScreen(url: String, title: String, favicon: Bitmap?)

        fun download(pendingDownload: PendingDownload)

        fun backgroundBrowser()

        fun launchIncognito(url: String?)

    }

    enum class CloseTabEvent {
        CLOSE_CURRENT,
        CLOSE_OTHERS,
        CLOSE_ALL
    }

    enum class BookmarkOptionEvent {
        NEW_TAB,
        BACKGROUND_TAB,
        INCOGNITO_TAB,
        SHARE,
        COPY_LINK,
        REMOVE,
        EDIT
    }

    enum class HistoryOptionEvent {
        NEW_TAB,
        BACKGROUND_TAB,
        INCOGNITO_TAB,
        SHARE,
        COPY_LINK,
        REMOVE,
    }

    enum class DownloadOptionEvent {
        DELETE,
        DELETE_ALL
    }

    enum class FolderOptionEvent {
        RENAME,
        REMOVE
    }

    enum class LinkLongPressEvent {
        NEW_TAB,
        BACKGROUND_TAB,
        INCOGNITO_TAB,
        SHARE,
        COPY_LINK
    }

    enum class ImageLongPressEvent {
        NEW_TAB,
        BACKGROUND_TAB,
        INCOGNITO_TAB,
        SHARE,
        COPY_LINK,
        DOWNLOAD
    }

    sealed class Action {
        data class LoadUrl(val url: String) : Action()

        object Panic : Action()
    }

}
