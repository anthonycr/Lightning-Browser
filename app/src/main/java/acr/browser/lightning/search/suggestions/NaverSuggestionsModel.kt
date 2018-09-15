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
 * The search suggestions provider for the Naver search engine.
 */
class NaverSuggestionsModel(application: Application) : BaseSuggestionsModel(application, UTF8) {

    private val searchSubtitle = application.getString(R.string.suggestion)

    // https://ac.search.naver.com/nx/ac?q=$query&q_enc=UTF-8&st=100&frm=nv&r_format=json&r_enc=UTF-8&r_unicode=0&t_koreng=1&ans=2&run=2&rev=4&con=1
    override fun createQueryUrl(query: String, language: String): HttpUrl =
        HttpUrl.Builder()
            .scheme("https")
            .host("ac.search.naver.com")
            .encodedPath("/nx/ac")
            .addQueryParameter("q", query)
            .addQueryParameter("q_enc", "UTF-8")
            .addQueryParameter("st", "100")
            .addQueryParameter("frm", "nv")
            .addQueryParameter("r_format", "json")
            .addQueryParameter("r_enc", "UTF-8")
            .addQueryParameter("r_unicode", "0")
            .addQueryParameter("t_koreng", "1")
            .addQueryParameter("ans", "2")
            .addQueryParameter("run", "2")
            .addQueryParameter("rev", "4")
            .addQueryParameter("con", "1")
            .build()

    override fun parseResults(responseBody: ResponseBody): List<HistoryItem> {
        return JSONObject(responseBody.string())
            .getJSONArray("items")
            .getJSONArray(0)
            .map { it as JSONArray }
            .map { it[0] as String }
            .map { HistoryItem("$searchSubtitle \"$it\"", it, R.drawable.ic_search) }
    }

}
