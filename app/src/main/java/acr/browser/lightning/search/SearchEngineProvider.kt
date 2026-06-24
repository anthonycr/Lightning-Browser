package acr.browser.lightning.search

import acr.browser.lightning.preference.UserPreferencesDataStore
import acr.browser.lightning.preference.datastore.getUnsafe
import acr.browser.lightning.search.engine.AskSearch
import acr.browser.lightning.search.engine.BaiduSearch
import acr.browser.lightning.search.engine.BaseSearchEngine
import acr.browser.lightning.search.engine.BingSearch
import acr.browser.lightning.search.engine.CustomSearch
import acr.browser.lightning.search.engine.DuckLiteSearch
import acr.browser.lightning.search.engine.DuckSearch
import acr.browser.lightning.search.engine.GoogleSearch
import acr.browser.lightning.search.engine.NaverSearch
import acr.browser.lightning.search.engine.StartPageMobileSearch
import acr.browser.lightning.search.engine.StartPageSearch
import acr.browser.lightning.search.engine.YahooSearch
import acr.browser.lightning.search.engine.YandexSearch
import acr.browser.lightning.search.suggestions.BaiduSuggestionsModel
import acr.browser.lightning.search.suggestions.DuckSuggestionsModel
import acr.browser.lightning.search.suggestions.GoogleSuggestionsModel
import acr.browser.lightning.search.suggestions.NaverSuggestionsModel
import acr.browser.lightning.search.suggestions.NoOpSuggestionsRepository
import acr.browser.lightning.search.suggestions.SuggestionsRepository
import dagger.Reusable
import javax.inject.Inject
import javax.inject.Provider

/**
 * The model that provides the search engine based
 * on the user's preference.
 */
@Reusable
class SearchEngineProvider @Inject constructor(
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val googleSuggestionsModel: Provider<GoogleSuggestionsModel>,
    private val duckSuggestionsModel: Provider<DuckSuggestionsModel>,
    private val baiduSuggestionsModel: Provider<BaiduSuggestionsModel>,
    private val naverSuggestionsModel: Provider<NaverSuggestionsModel>,
) {

    /**
     * Provide the [SuggestionsRepository] that maps to the user's current preference.
     */
    fun provideSearchSuggestions(): SuggestionsRepository =
        when (userPreferencesDataStore.searchSuggestionChoice.getUnsafe()) {
            Suggestions.NONE -> NoOpSuggestionsRepository()
            Suggestions.GOOGLE -> googleSuggestionsModel.get()
            Suggestions.DUCK -> duckSuggestionsModel.get()
            Suggestions.BAIDU -> baiduSuggestionsModel.get()
            Suggestions.NAVER -> naverSuggestionsModel.get()
        }

    /**
     * Provide the [BaseSearchEngine] that maps to the user's current preference.
     */
    fun provideSearchEngine(): BaseSearchEngine =
        when (userPreferencesDataStore.searchChoice.getUnsafe()) {
            SearchEngineChoice.CUSTOM -> CustomSearch(userPreferencesDataStore.searchUrl.getUnsafe())
            SearchEngineChoice.GOOGLE -> GoogleSearch()
            SearchEngineChoice.ASK -> AskSearch()
            SearchEngineChoice.BING -> BingSearch()
            SearchEngineChoice.YAHOO -> YahooSearch()
            SearchEngineChoice.START_PAGE -> StartPageSearch()
            SearchEngineChoice.START_PAGE_MOBILE -> StartPageMobileSearch()
            SearchEngineChoice.DUCK -> DuckSearch()
            SearchEngineChoice.DUCK_LITE -> DuckLiteSearch()
            SearchEngineChoice.BAIDU -> BaiduSearch()
            SearchEngineChoice.YANDEX -> YandexSearch()
            SearchEngineChoice.NAVER -> NaverSearch()
        }

    /**
     * Provide a list of all supported search engines.
     */
    suspend fun provideAllSearchEngines(): List<BaseSearchEngine> = listOf(
        CustomSearch(userPreferencesDataStore.searchUrl.get()),
        GoogleSearch(),
        AskSearch(),
        BingSearch(),
        YahooSearch(),
        StartPageSearch(),
        StartPageMobileSearch(),
        DuckSearch(),
        DuckLiteSearch(),
        BaiduSearch(),
        YandexSearch(),
        NaverSearch()
    )

}
