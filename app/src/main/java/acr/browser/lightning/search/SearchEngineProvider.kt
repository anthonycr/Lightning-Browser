package acr.browser.lightning.search

import acr.browser.lightning.BrowserApp
import acr.browser.lightning.preference.PreferenceManager
import acr.browser.lightning.search.engine.*
import javax.inject.Inject

/**
 * The model that provides the search engine based
 * on the user's preference.
 */
class SearchEngineProvider @Inject constructor() {

    @Inject internal lateinit var preferenceManager: PreferenceManager

    init {
        BrowserApp.appComponent.inject(this)
    }

    fun getCurrentSearchEngine(): BaseSearchEngine =
            when (preferenceManager.searchChoice) {
                0 -> CustomSearch(preferenceManager.searchUrl)
                1 -> GoogleSearch()
                2 -> AskSearch()
                3 -> BingSearch()
                4 -> YahooSearch()
                5 -> StartPageSearch()
                6 -> StartPageMobileSearch()
                7 -> DuckSearch()
                8 -> DuckLiteSearch()
                9 -> BaiduSearch()
                10 -> YandexSearch()
                else -> GoogleSearch()
            }

    fun mapSearchEngineToPreferenceIndex(searchEngine: BaseSearchEngine): Int =
            when (searchEngine) {
                is CustomSearch -> 0
                is GoogleSearch -> 1
                is AskSearch -> 2
                is BingSearch -> 3
                is YahooSearch -> 4
                is StartPageSearch -> 5
                is StartPageMobileSearch -> 6
                is DuckSearch -> 7
                is DuckLiteSearch -> 8
                is BaiduSearch -> 9
                is YandexSearch -> 10
                else -> throw UnsupportedOperationException("Unknown search engine provided: " + searchEngine.javaClass)
            }

    fun getAllSearchEngines(): List<BaseSearchEngine> = listOf(
            CustomSearch(preferenceManager.searchUrl),
            GoogleSearch(),
            AskSearch(),
            BingSearch(),
            YahooSearch(),
            StartPageSearch(),
            StartPageMobileSearch(),
            DuckSearch(),
            DuckLiteSearch(),
            BaiduSearch(),
            YandexSearch()
    )

}
