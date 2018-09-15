package acr.browser.lightning.search.suggestions

import acr.browser.lightning.R
import acr.browser.lightning.constant.UTF8
import acr.browser.lightning.database.HistoryItem
import android.app.Application
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/**
 * Search suggestions provider for Google search engine.
 */
class GoogleSuggestionsModel(
    httpClient: OkHttpClient,
    requestFactory: RequestFactory,
    application: Application
) : BaseSuggestionsModel(httpClient, requestFactory, UTF8) {

    private val searchSubtitle = application.getString(R.string.suggestion)

    // https://suggestqueries.google.com/complete/search?output=toolbar&hl={language}&q={query}
    override fun createQueryUrl(query: String, language: String): HttpUrl = HttpUrl.Builder()
        .scheme("https")
        .host("suggestqueries.google.com")
        .encodedPath("/complete/search")
        .addQueryParameter("output", "toolbar")
        .addQueryParameter("hl", language)
        .addQueryParameter("q", query)
        .build()

    @Throws(Exception::class)
    override fun parseResults(responseBody: ResponseBody): List<HistoryItem> {
        parser.setInput(responseBody.byteStream(), UTF8)

        val mutableList = mutableListOf<HistoryItem>()
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && "suggestion" == parser.name) {
                val suggestion = parser.getAttributeValue(null, "data")
                mutableList.add(HistoryItem("$searchSubtitle \"$suggestion\"", suggestion, R.drawable.ic_search))
            }
            eventType = parser.next()
        }

        return mutableList
    }

    companion object {

        private val parser by lazy(
            XmlPullParserFactory.newInstance().apply {
                isNamespaceAware = true
            }::newPullParser
        )

    }
}
