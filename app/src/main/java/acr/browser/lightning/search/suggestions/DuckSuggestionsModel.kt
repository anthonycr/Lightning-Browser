package acr.browser.lightning.search.suggestions

import acr.browser.lightning.R
import acr.browser.lightning.constant.Constants
import acr.browser.lightning.database.HistoryItem
import acr.browser.lightning.utils.FileUtils
import android.app.Application
import org.json.JSONArray
import java.io.InputStream

/**
 * The search suggestions provider for the DuckDuckGo search engine.
 */
class DuckSuggestionsModel(application: Application) : BaseSuggestionsModel(application, Constants.UTF8) {

    private val searchSubtitle = application.getString(R.string.suggestion)

    override fun createQueryUrl(query: String, language: String): String {
        return "https://duckduckgo.com/ac/?q=$query"
    }

    @Throws(Exception::class)
    override fun parseResults(inputStream: InputStream, results: MutableList<HistoryItem>) {
        val content = FileUtils.readStringFromStream(inputStream, Constants.UTF8)
        val jsonArray = JSONArray(content)

        var n = 0
        val size = jsonArray.length()
        while (n < size && n < BaseSuggestionsModel.MAX_RESULTS) {
            val `object` = jsonArray.getJSONObject(n)
            val suggestion = `object`.getString("phrase")
            results.add(HistoryItem(searchSubtitle + " \"$suggestion\"",
                    suggestion, R.drawable.ic_search))
            n++
        }
    }

}
