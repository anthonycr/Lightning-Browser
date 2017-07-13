package acr.browser.lightning.search.suggestions

import acr.browser.lightning.database.HistoryItem
import acr.browser.lightning.utils.FileUtils
import acr.browser.lightning.utils.Utils
import android.app.Application
import android.text.TextUtils
import android.util.Log
import okhttp3.*
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.net.URL
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * The base search suggestions API. Provides common
 * fetching and caching functionality for each potential
 * suggestions provider.
 */
abstract class BaseSuggestionsModel internal constructor(application: Application, private val encoding: String) {

    private val httpClient: OkHttpClient
    private val cacheControl = CacheControl.Builder().maxStale(1, TimeUnit.DAYS).build()

    /**
     * Create a URL for the given query in the given language.

     * @param query    the query that was made.
     * *
     * @param language the locale of the user.
     * *
     * @return should return a URL that can be fetched using a GET.
     */
    protected abstract fun createQueryUrl(query: String, language: String): String

    /**
     * Parse the results of an input stream into a list of [HistoryItem].

     * @param inputStream the raw input to parse.
     * *
     * @param results     the list to populate.
     * *
     * @throws Exception throw an exception if anything goes wrong.
     */
    @Throws(Exception::class)
    protected abstract fun parseResults(inputStream: InputStream, results: MutableList<HistoryItem>)

    init {
        val suggestionsCache = File(application.cacheDir, "suggestion_responses")
        httpClient = OkHttpClient.Builder()
                .cache(Cache(suggestionsCache, FileUtils.megabytesToBytes(1)))
                .addNetworkInterceptor(REWRITE_CACHE_CONTROL_INTERCEPTOR)
                .build()
    }

    /**
     * Retrieves the results for a query.

     * @param rawQuery the raw query to retrieve the results for.
     * *
     * @return a list of history items for the query.
     */
    fun fetchResults(rawQuery: String): List<HistoryItem> {
        val filter = ArrayList<HistoryItem>(5)

        val query: String
        try {
            query = URLEncoder.encode(rawQuery, encoding)
        } catch (e: UnsupportedEncodingException) {
            Log.e(TAG, "Unable to encode the URL", e)

            return filter
        }

        val inputStream = downloadSuggestionsForQuery(query, language) ?: return filter

        try {
            parseResults(inputStream, filter)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to parse results", e)
        } finally {
            Utils.close(inputStream)
        }

        return filter
    }

    /**
     * This method downloads the search suggestions for the specific query.
     * NOTE: This is a blocking operation, do not fetchResults on the UI thread.

     * @param query the query to get suggestions for
     * *
     * @return the cache file containing the suggestions
     */
    private fun downloadSuggestionsForQuery(query: String, language: String): InputStream? {
        val queryUrl = createQueryUrl(query, language)

        try {
            val url = URL(queryUrl)

            // OkHttp automatically gzips requests
            val suggestionsRequest = Request.Builder().url(url)
                    .addHeader("Accept-Charset", encoding)
                    .cacheControl(cacheControl)
                    .build()

            val suggestionsResponse = httpClient.newCall(suggestionsRequest).execute()

            val responseBody = suggestionsResponse.body()

            return responseBody?.byteStream()
        } catch (exception: IOException) {
            Log.e(TAG, "Problem getting search suggestions", exception)
        }

        return null
    }

    companion object {

        private val TAG = "BaseSuggestionsModel"

        internal val MAX_RESULTS = 5
        private val INTERVAL_DAY = TimeUnit.DAYS.toSeconds(1)
        private val DEFAULT_LANGUAGE = "en"

        private val language by lazy {
            var lang = Locale.getDefault().language
            if (TextUtils.isEmpty(lang)) {
                lang = DEFAULT_LANGUAGE
            }

            lang
        }

        private val REWRITE_CACHE_CONTROL_INTERCEPTOR = Interceptor { chain ->
            val originalResponse = chain.proceed(chain.request())
            originalResponse.newBuilder()
                    .header("cache-control", "max-age=$INTERVAL_DAY, max-stale=$INTERVAL_DAY")
                    .build()
        }
    }

}
