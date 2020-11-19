package acr.browser.lightning._browser2

import acr.browser.lightning._browser2.tab.TabModel
import acr.browser.lightning.view.TabInitializer
import android.graphics.Bitmap
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single

/**
 * Created by anthonycr on 9/11/20.
 */


interface BrowserContract {

    interface View {

        fun renderState(viewState: BrowserViewState)

        fun showAddBookmarkDialog(title: String, url: String, folders: List<String>)

        fun showEditBookmarkDialog(title: String, url: String, folder: String, folders: List<String>)

        fun showEditFolderDialog(title: String)

        fun openBookmarkDrawer()

        fun openTabDrawer()
    }

    interface Model {

        fun deleteTab(id: Int): Completable

        fun createTab(tabInitializer: TabInitializer): Single<TabModel>

        fun selectTab(id: Int): TabModel

        fun initializeTabs(): Maybe<List<TabModel>>

        fun freeze()

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

        fun backgroundBrowser()
    }



}
