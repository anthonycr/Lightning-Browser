package acr.browser.lightning.search.suggestions

import acr.browser.lightning.R
import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.constant.UTF8
import acr.browser.lightning.database.SearchSuggestion
import acr.browser.lightning.di.SuggestionsClient
import acr.browser.lightning.log.Logger
import acr.browser.lightning.resources.ResourceProvider
import kotlinx.coroutines.Deferred
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.serializer
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import java.util.Locale
import javax.inject.Inject

/**
 * The search suggestions provider for the Naver search engine.
 */
class NaverSuggestionsModel @Inject constructor(
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
    private val serializer = Json.serializersModule.serializer<NaverSuggestion>()

    // https://ac.search.naver.com/nx/ac?q=$query&q_enc=UTF-8&st=100&frm=nv&r_format=json&r_enc=UTF-8&r_unicode=0&t_koreng=1&ans=2&run=2&rev=4&con=1
    override fun createQueryUrl(query: String, language: String): HttpUrl =
        HttpUrl.Builder()
            .scheme("https")
            .host("ac.search.naver.com")
            .encodedPath("/nx/ac")
            .addEncodedQueryParameter("q", query)
            .addQueryParameter("q_enc", "UTF-8")
            .addQueryParameter("st", "100")
            .addQueryParameter("frm", "nv")
            .addQueryParameter("r_format", "json")
            .addQueryParameter("r_enc", "UTF-8")
            .addQueryParameter("r_unicode", "0")
            .addQueryParameter("t_koreng", "1")
            .addQueryParameter("ans", "2")
            .addQueryParameter("run", "2")
            .addQueryParameter("rev", "4")
            .addQueryParameter("con", "1")
            .build()

    @OptIn(ExperimentalSerializationApi::class)
    override fun parseResults(responseBody: ResponseBody): List<SearchSuggestion> {
        return Json.decodeFromStream(serializer, responseBody.byteStream()).items[0].map {
            SearchSuggestion("$searchSubtitle \"${it[0]}\"", it[0])
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @JsonIgnoreUnknownKeys
    @Serializable
    data class NaverSuggestion(
        @SerialName("items")
        val items: List<List<List<String>>>
    )
}
