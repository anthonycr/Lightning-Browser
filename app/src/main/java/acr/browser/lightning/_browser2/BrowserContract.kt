package acr.browser.lightning._browser2

import acr.browser.lightning._browser2.tab.TabModel
import acr.browser.lightning.view.TabInitializer
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

    }

    interface Model {

        fun deleteTab(id: Int): Completable

        fun createTab(tabInitializer: TabInitializer): Single<TabModel>

        fun selectTab(id: Int): TabModel

        fun initializeTabs(): Maybe<List<TabModel>>

        val tabsList: List<TabModel>

        fun tabsListChanges(): Observable<List<TabModel>>

    }

    interface Navigator {

        fun openSettings()

        fun openReaderMode(url: String)

        fun sharePage(url: String, title: String?)

        fun copyPageLink(url: String)
    }



}
