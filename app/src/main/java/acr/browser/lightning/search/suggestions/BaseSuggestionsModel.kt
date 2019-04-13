package acr.browser.lightning.search.suggestions

import acr.browser.lightning.database.SearchSuggestion
import acr.browser.lightning.extensions.safeUse
import acr.browser.lightning.log.Logger
import io.reactivex.Single
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.*

/**
 * The base search suggestions API. Provides common fetching and caching functionality for each
 * potential suggestions provider.
 */
abstract class BaseSuggestionsModel internal constructor(
    private val httpClient: OkHttpClient,
    private val requestFactory: RequestFactory,
    private val encoding: String,
    private val logger: Logger
) : SuggestionsRepository {

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

    override fun resultsForSearch(rawQuery: String): Single<List<SearchSuggestion>> = Single.fromCallable {
        val query = try {
            URLEncoder.encode(rawQuery, encoding)
        } catch (throwable: UnsupportedEncodingException) {
            logger.log(TAG, "Unable to encode the URL", throwable)

            return@fromCallable emptyList<SearchSuggestion>()
        }

        return@fromCallable downloadSuggestionsForQuery(query, language)
            ?.let(Response::body)
            ?.safeUse(::parseResults)
            ?.take(MAX_RESULTS)
    }

    /**
     * This method downloads the search suggestions for the specific query.
     * NOTE: This is a blocking operation, do not fetchResults on the UI thread.
     *
     * @param query the query to get suggestions for
     *
     * @return the cache file containing the suggestions
     */
    private fun downloadSuggestionsForQuery(query: String, language: String): Response? {
        val queryUrl = createQueryUrl(query, language)
        val request = requestFactory.createSuggestionsRequest(queryUrl, encoding)
        return try {
            httpClient.newCall(request).execute()
        } catch (exception: IOException) {
            logger.log(TAG, "Problem getting search suggestions", exception)
            null
        }
    }

    companion object {

        private const val TAG = "BaseSuggestionsModel"

        private const val MAX_RESULTS = 5
        private const val DEFAULT_LANGUAGE = "en"

        private val language by lazy {
            Locale.getDefault().language.takeIf(String::isNotEmpty) ?: DEFAULT_LANGUAGE
        }

    }

}
