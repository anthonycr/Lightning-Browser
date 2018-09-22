package acr.browser.lightning.html.history

import acr.browser.lightning.R
import acr.browser.lightning.constant.FILE
import acr.browser.lightning.database.history.HistoryRepository
import acr.browser.lightning.html.HtmlPageFactory
import android.app.Application
import io.reactivex.Completable
import io.reactivex.Single
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.File
import java.io.FileWriter
import javax.inject.Inject

/**
 * Factory for the history page.
 */
class HistoryPageFactory @Inject constructor(
    private val listPageReader: ListPageReader,
    private val application: Application,
    private val historyRepository: HistoryRepository
) : HtmlPageFactory {

    private val title = application.getString(R.string.action_history)

    override fun buildPage(): Single<String> = historyRepository
        .lastHundredVisitedHistoryEntries()
        .map { list ->
            Jsoup.parse(listPageReader.provideHtml())
                .apply {
                    title(title)
                    body().also { body ->
                        val repeatedElement = body.getElementById("repeated").also(Element::remove)
                        body.getElementById("content").also { content ->
                            list.forEach {
                                content.appendChild(repeatedElement.clone().apply {
                                    getElementsByTag("a").first().attr("href", it.url)
                                    getElementById("title").text(it.title)
                                    getElementById("url").text(it.url)
                                })
                            }
                        }
                    }
                }
                .outerHtml()

        }
        .map { content -> Pair(createHistoryPage(), content) }
        .doOnSuccess { (page, content) ->
            FileWriter(page, false).use { it.write(content) }
        }
        .map { (page, _) -> "$FILE$page" }

    /**
     * Use this observable to immediately delete the history page. This will clear the cached
     * history page that was stored on file.
     *
     * @return a completable that deletes the history page when subscribed to.
     */
    fun deleteHistoryPage(): Completable = Completable.fromAction {
        with(createHistoryPage()) {
            if (exists()) {
                delete()
            }
        }
    }

    private fun createHistoryPage() = File(application.filesDir, FILENAME)

    companion object {
        const val FILENAME = "history.html"
    }

}
