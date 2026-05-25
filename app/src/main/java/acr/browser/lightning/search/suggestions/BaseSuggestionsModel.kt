package acr.browser.lightning.search.suggestions

import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.database.SearchSuggestion
import acr.browser.lightning.extensions.safeUse
import acr.browser.lightning.log.Logger
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.Locale

/**
 * The base search suggestions API. Provides common fetching and caching functionality for each
 * potential suggestions provider.
 */
abstract class BaseSuggestionsModel internal constructor(
    private val okHttpClientDeferred: Deferred<OkHttpClient>,
    private val requestFactory: RequestFactory,
    private val encoding: String,
    locale: Locale,
    private val logger: Logger,
    private val coroutineDispatchers: CoroutineDispatchers,
) : SuggestionsRepository {

    private val language = locale.language.takeIf(String::isNotEmpty) ?: DEFAULT_LANGUAGE

    /**
     * Create a URL for the given query in the given language.
     *
     * @param query    the query that was made.
     * @param language the locale of the user.
     * @return should return a [HttpUrl] that can be fetched using a GET.
     */
    abstract fun createQueryUrl(query: String, language: String): HttpUrl

    /**
     * Parse the results of an input stream into a list of [SearchSuggestion].
     *
     * @param responseBody the raw [ResponseBody] to parse.
     */
    @Throws(Exception::class)
    protected abstract fun parseResults(responseBody: ResponseBody): List<SearchSuggestion>

    override suspend fun resultsForSearch(
        rawQuery: String
    ): List<SearchSuggestion> = withContext(coroutineDispatchers.network) {
        val okHttpClient = okHttpClientDeferred.await()

        val query = try {
            URLEncoder.encode(rawQuery, encoding)
        } catch (throwable: UnsupportedEncodingException) {
            logger.log(TAG, "Unable to encode the URL", throwable)

            return@withContext emptyList<SearchSuggestion>()
        }

        okHttpClient.downloadSuggestionsForQuery(query, language)
            ?.body
            ?.safeUse(::parseResults)
            ?.take(MAX_RESULTS) ?: emptyList()
    }

    /**
     * This method downloads the search suggestions for the specific query.
     * NOTE: This is a blocking operation, do not fetchResults on the UI thread.
     *
     * @param query the query to get suggestions for
     *
     * @return the cache file containing the suggestions
     */
    private fun OkHttpClient.downloadSuggestionsForQuery(
        query: String,
        language: String
    ): Response? {
        val queryUrl = createQueryUrl(query, language)
        val request = requestFactory.createSuggestionsRequest(queryUrl, encoding)
        return try {
            newCall(request).execute()
        } catch (exception: IOException) {
            logger.log(TAG, "Problem getting search suggestions", exception)
            null
        }
    }

    companion object {

        private const val TAG = "BaseSuggestionsModel"

        private const val MAX_RESULTS = 5
        private const val DEFAULT_LANGUAGE = "en"

    }

}
