package acr.browser.lightning.search.suggestions

import acr.browser.lightning.R
import acr.browser.lightning.constant.Constants
import acr.browser.lightning.database.HistoryItem
import android.app.Application
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedInputStream
import java.io.InputStream

/**
 * Search suggestions provider for Google search engine.
 */
class GoogleSuggestionsModel(application: Application) : BaseSuggestionsModel(application, Constants.UTF8) {

    private val searchSubtitle = application.getString(R.string.suggestion)

    override fun createQueryUrl(query: String, language: String): String {
        return "https://suggestqueries.google.com/complete/search?output=toolbar&hl=$language&q=$query"
    }

    @Throws(Exception::class)
    override fun parseResults(inputStream: InputStream, results: MutableList<HistoryItem>) {
        val bufferedInput = BufferedInputStream(inputStream)

        parser.setInput(bufferedInput, Constants.UTF8)

        var eventType = parser.eventType
        var counter = 0
        while (eventType != XmlPullParser.END_DOCUMENT && counter < BaseSuggestionsModel.MAX_RESULTS) {
            if (eventType == XmlPullParser.START_TAG && "suggestion" == parser.name) {
                val suggestion = parser.getAttributeValue(null, "data")
                results.add(HistoryItem(searchSubtitle + " \"$suggestion\"",
                        suggestion, R.drawable.ic_search))
                counter++
            }
            eventType = parser.next()
        }
    }

    companion object {

        private val parser by lazy {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true

            factory.newPullParser()
        }

    }
}
