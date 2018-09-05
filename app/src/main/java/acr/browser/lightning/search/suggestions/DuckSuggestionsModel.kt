package acr.browser.lightning.search.suggestions

import acr.browser.lightning.R
import acr.browser.lightning.constant.UTF8
import acr.browser.lightning.database.HistoryItem
import acr.browser.lightning.extensions.map
import android.app.Application
import okhttp3.HttpUrl
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * The search suggestions provider for the DuckDuckGo search engine.
 */
class DuckSuggestionsModel(application: Application) : BaseSuggestionsModel(application, UTF8) {

    private val searchSubtitle = application.getString(R.string.suggestion)

    // https://duckduckgo.com/ac/?q={query}
    override fun createQueryUrl(query: String, language: String): HttpUrl = HttpUrl.Builder()
        .scheme("https")
        .host("duckduckgo.com")
        .encodedPath("/ac/")
        .addQueryParameter("q", query)
        .build()

    @Throws(Exception::class)
    override fun parseResults(responseBody: ResponseBody): List<HistoryItem> {
        val jsonArray = JSONArray(responseBody.string())

        return jsonArray
            .map { it as JSONObject }
            .map { it.getString("phrase") }
            .map { HistoryItem("$searchSubtitle \"$it\"", it, R.drawable.ic_search) }
    }

}
