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
 * The search suggestions provider for the DuckDuckGo search engine.
 */
class DuckSuggestionsModel @Inject constructor(
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
    private val serializer = Json.serializersModule.serializer<List<DuckSuggestion>>()

    // https://duckduckgo.com/ac/?q={query}
    override fun createQueryUrl(query: String, language: String): HttpUrl = HttpUrl.Builder()
        .scheme("https")
        .host("duckduckgo.com")
        .encodedPath("/ac/")
        .addEncodedQueryParameter("q", query)
        .build()

    @OptIn(ExperimentalSerializationApi::class)
    @Throws(Exception::class)
    override fun parseResults(responseBody: ResponseBody): List<SearchSuggestion> {
        return Json.decodeFromStream(serializer, responseBody.byteStream())
            .map { SearchSuggestion("$searchSubtitle \"$it\"", it.phrase) }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @JsonIgnoreUnknownKeys
    @Serializable
    data class DuckSuggestion(
        @SerialName("phrase")
        val phrase: String
    )
}
