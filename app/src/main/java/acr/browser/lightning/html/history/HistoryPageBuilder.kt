package acr.browser.lightning.html.history

import acr.browser.lightning.R
import acr.browser.lightning.database.HistoryEntry
import android.app.Application
import com.anthonycr.mezzanine.MezzanineGenerator
import org.jsoup.Jsoup

/**
 * The builder for the history page.
 */
class HistoryPageBuilder(private val app: Application) {

    fun buildPage(historyList: List<HistoryEntry>): String {
        val html = MezzanineGenerator.ListPageReader().provideHtml()

        val document = Jsoup.parse(html).apply {
            title(app.getString(R.string.action_history))
        }

        val body = document.body()
        val repeatableElement = body.getElementById("repeated")
        val container = body.getElementById("content")
        repeatableElement.remove()

        historyList.forEach {
            val newElement = repeatableElement.clone()

            newElement.getElementsByTag("a").first().attr("href", it.url)
            newElement.getElementById("title").text(it.title)
            newElement.getElementById("url").text(it.url)
            container.appendChild(newElement)
        }

        return document.outerHtml()
    }

}
