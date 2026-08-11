package acr.browser.lightning.search.suggestions

import acr.browser.lightning.R
import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.constant.UTF8
import acr.browser.lightning.database.SearchSuggestion
import acr.browser.lightning.di.SuggestionsClient
import acr.browser.lightning.extensions.map
import acr.browser.lightning.log.Logger
import acr.browser.lightning.resources.ResourceProvider
import kotlinx.coroutines.Deferred
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import javax.inject.Inject

/**
 * Search suggestions for Kagi. Requires being logged into Kagi.
 *
 * TODO: Provide logged in cookie from browser as request header.
 * TODO: Provide UI info that login is required.
 */
class KagiSuggestionsModel @Inject constructor(
    @SuggestionsClient okHttpClient: Deferred<@JvmSuppressWildcards OkHttpClient>,
    requestFactory: RequestFactory,
    locale: Locale,
    resourceProvider: ResourceProvider,
    logger: Logger,
    coroutineDispatchers: CoroutineDispatchers,
) : BaseSuggestionsModel(
    okHttpClient,
    requestFactory,
    UTF8,
    locale,
    logger,
    coroutineDispatchers
) {
    private val searchSubtitle = resourceProvider.stringResource(R.string.suggestion)

    // https://kagi.com/autosuggest?q={query}
    override fun createQueryUrl(query: String, language: String): HttpUrl = HttpUrl.Builder()
        .scheme("https")
        .host("kagi.com")
        .encodedPath("/autosuggest")
        .addEncodedQueryParameter("q", query)
        .build()

    override fun parseResults(responseBody: ResponseBody): List<SearchSuggestion> {
        return JSONArray(responseBody.toString())
            .map { it as JSONObject }
            .map { it.getString("t") }
            .map { SearchSuggestion("$searchSubtitle \"$it\"", it) }
    }

}
