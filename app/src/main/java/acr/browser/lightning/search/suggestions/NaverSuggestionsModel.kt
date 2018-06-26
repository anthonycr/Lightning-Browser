package acr.browser.lightning.search.suggestions

import acr.browser.lightning.R
import acr.browser.lightning.constant.UTF8
import acr.browser.lightning.database.HistoryItem
import acr.browser.lightning.extensions.map
import acr.browser.lightning.search.engine.NaverSearch
import acr.browser.lightning.utils.FileUtils
import android.app.Application
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

/**
 * The search suggestions provider for the Naver search engine.
 */
class NaverSuggestionsModel(application: Application) : BaseSuggestionsModel(application, UTF8) {

    private val searchSubtitle = application.getString(R.string.suggestion)

    override fun createQueryUrl(query: String, language: String): String =
            "https://ac.search.naver.com/nx/ac?q=$query&q_enc=UTF-8&st=100&frm=nv&r_format=json&r_enc=UTF-8&r_unicode=0&t_koreng=1&ans=2&run=2&rev=4&con=1"

    @Throws(Exception::class)
    override fun parseResults(inputStream: InputStream): List<HistoryItem> {
        val content = FileUtils.readStringFromStream(inputStream, UTF8)
        val jsonobj = JSONObject(content)

        return jsonobj.getJSONArray("items")
                .getJSONArray(0)
                .map { it as JSONArray }
                .map { it.get(0) as String }
                .map { HistoryItem(
                        "${NaverSearch().queryUrl}$it",
                        it,
                        R.drawable.ic_search)
                }
    }

}
