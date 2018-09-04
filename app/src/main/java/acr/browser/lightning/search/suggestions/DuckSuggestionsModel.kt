package acr.browser.lightning.search.suggestions

import acr.browser.lightning.R
import acr.browser.lightning.constant.UTF8
import acr.browser.lightning.database.HistoryItem
import acr.browser.lightning.extensions.map
import acr.browser.lightning.utils.FileUtils
import android.app.Application
import okhttp3.HttpUrl
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

/**
 * The search suggestions provider for the DuckDuckGo search engine.
 */
class DuckSuggestionsModel(application: Application) : BaseSuggestionsModel(application, UTF8) {

    private val searchSubtitle = application.getString(R.string.suggestion)

    override fun createQueryUrl(query: String, language: String): HttpUrl? = HttpUrl.parse(
        "https://duckduckgo.com/ac/?q=$query"
    )

    @Throws(Exception::class)
    override fun parseResults(inputStream: InputStream): List<HistoryItem> {
        val content = FileUtils.readStringFromStream(inputStream, UTF8)
        val jsonArray = JSONArray(content)

        return jsonArray
            .map { it as JSONObject }
            .map { it.getString("phrase") }
            .map { HistoryItem("$searchSubtitle \"$it\"", it, R.drawable.ic_search) }
    }

}
