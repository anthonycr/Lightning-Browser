package acr.browser.lightning.html.download

import acr.browser.lightning.R
import acr.browser.lightning.constant.Constants
import acr.browser.lightning.database.downloads.DownloadItem
import android.app.Application
import com.anthonycr.mezzanine.MezzanineGenerator
import org.jsoup.Jsoup

/**
 * The builder for the download page.
 */
class DownloadPageBuilder(private val app: Application,
                          private val storageDirectory: String) {

    fun buildPage(downloadList: List<DownloadItem>): String {
        val html = MezzanineGenerator.ListPageReader().provideHtml()

        val document = Jsoup.parse(html).apply {
            title(app.getString(R.string.action_downloads))
        }

        val body = document.body()
        val repeatableElement = body.getElementById("repeated")
        val container = body.getElementById("content")
        repeatableElement.remove()

        downloadList.forEach {
            val newElement = repeatableElement.clone()

            newElement.getElementsByTag("a").first().attr("href", createFileUrl(it.title))
            newElement.getElementById("title").text(createFileTitle(it))
            newElement.getElementById("url").text(it.url)
            container.appendChild(newElement)
        }

        return document.outerHtml()
    }

    private fun createFileUrl(fileName: String): String =
            "${Constants.FILE}$storageDirectory/$fileName"

    private fun createFileTitle(downloadItem: DownloadItem): String {
        val contentSize = if (downloadItem.contentSize.isNotBlank()) {
            "[${downloadItem.contentSize}]"
        } else {
            ""
        }

        return "${downloadItem.title} $contentSize"
    }

}