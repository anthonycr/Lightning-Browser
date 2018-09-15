package acr.browser.lightning.search.suggestions

import acr.browser.lightning.R
import acr.browser.lightning.constant.UTF8
import acr.browser.lightning.database.HistoryItem
import acr.browser.lightning.extensions.map
import android.app.Application
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * The search suggestions provider for the DuckDuckGo search engine.
 */
class DuckSuggestionsModel(
    httpClient: OkHttpClient,
    requestFactory: RequestFactory,
    application: Application
) : BaseSuggestionsModel(httpClient, requestFactory, UTF8) {

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
        return JSONArray(responseBody.string())
            .map { it as JSONObject }
            .map { it.getString("phrase") }
            .map { HistoryItem("$searchSubtitle \"$it\"", it, R.drawable.ic_search) }
    }

}
