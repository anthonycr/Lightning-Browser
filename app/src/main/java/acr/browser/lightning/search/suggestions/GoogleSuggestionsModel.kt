package acr.browser.lightning.search.suggestions

import acr.browser.lightning.R
import acr.browser.lightning.constant.UTF8
import acr.browser.lightning.database.SearchSuggestion
import acr.browser.lightning.extensions.preferredLocale
import acr.browser.lightning.log.Logger
import android.app.Application
import io.reactivex.Single
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/**
 * Search suggestions provider for Google search engine.
 */
class GoogleSuggestionsModel(
    okHttpClient: Single<OkHttpClient>,
    requestFactory: RequestFactory,
    application: Application,
    logger: Logger
) : BaseSuggestionsModel(okHttpClient, requestFactory, UTF8, application.preferredLocale, logger) {

    private val searchSubtitle = application.getString(R.string.suggestion)

    // https://suggestqueries.google.com/complete/search?output=toolbar&hl={language}&q={query}
    override fun createQueryUrl(query: String, language: String): HttpUrl = HttpUrl.Builder()
        .scheme("https")
        .host("suggestqueries.google.com")
        .encodedPath("/complete/search")
        .addQueryParameter("output", "toolbar")
        .addQueryParameter("hl", language)
        .addEncodedQueryParameter("q", query)
        .build()

    @Throws(Exception::class)
    override fun parseResults(responseBody: ResponseBody): List<SearchSuggestion> {
        parser.setInput(responseBody.byteStream(), UTF8)

        val suggestions = mutableListOf<SearchSuggestion>()
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && "suggestion" == parser.name) {
                val suggestion = parser.getAttributeValue(null, "data")
                suggestions.add(SearchSuggestion("$searchSubtitle \"$suggestion\"", suggestion))
            }
            eventType = parser.next()
        }

        return suggestions
    }

    companion object {

        // Converting to a lambda results in pulling the newInstance call out of the lazy.
        @Suppress("ConvertLambdaToReference")
        private val parser by lazy {
            XmlPullParserFactory.newInstance().apply {
                isNamespaceAware = true
            }.newPullParser()
        }

    }
}
