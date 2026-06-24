package acr.browser.lightning.html.download

import acr.browser.lightning.R
import acr.browser.lightning.browser.di.GeneratedHtmlDir
import acr.browser.lightning.browser.theme.ThemeProvider
import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.constant.FILE
import acr.browser.lightning.database.downloads.DownloadEntry
import acr.browser.lightning.database.downloads.DownloadsRepository
import acr.browser.lightning.html.HtmlPageFactory
import acr.browser.lightning.html.ListPageReader
import acr.browser.lightning.html.jsoup.andBuild
import acr.browser.lightning.html.jsoup.body
import acr.browser.lightning.html.jsoup.clone
import acr.browser.lightning.html.jsoup.findId
import acr.browser.lightning.html.jsoup.id
import acr.browser.lightning.html.jsoup.parse
import acr.browser.lightning.html.jsoup.removeElement
import acr.browser.lightning.html.jsoup.style
import acr.browser.lightning.html.jsoup.tag
import acr.browser.lightning.html.jsoup.title
import acr.browser.lightning.preference.UserPreferencesDataStore
import acr.browser.lightning.utils.ThreadSafeFileProvider
import android.app.Application
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import javax.inject.Inject

/**
 * The factory for the downloads page.
 */
class DownloadPageFactory @Inject constructor(
    private val application: Application,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val manager: DownloadsRepository,
    private val listPageReader: ListPageReader,
    private val themeProvider: ThemeProvider,
    private val coroutineDispatchers: CoroutineDispatchers,
    @GeneratedHtmlDir private val generatedHtmlDir: ThreadSafeFileProvider,
) : HtmlPageFactory {

    private fun Int.toColor(): String {
        val string = Integer.toHexString(this)

        return string.substring(2) + string.substring(0, 2)
    }

    private val backgroundColor: String
        get() = themeProvider.color(R.attr.colorPrimary).toColor()
    private val dividerColor: String
        get() = themeProvider.color(R.attr.autoCompleteBackgroundColor).toColor()
    private val textColor: String
        get() = themeProvider.color(R.attr.autoCompleteTitleColor).toColor()
    private val subtitleColor: String
        get() = themeProvider.color(R.attr.autoCompleteUrlColor).toColor()

    override suspend fun buildPage(): String = withContext(coroutineDispatchers.io) {
        val downloads = manager.getAllDownloads().map { it to createFileUrl(it.title) }
        val content = parse(listPageReader.provideHtml()) andBuild {
            title { application.getString(R.string.action_downloads) }
            style { content ->
                content.replace("--body-bg: {COLOR}", "--body-bg: #$backgroundColor;")
                    .replace("--divider-color: {COLOR}", "--divider-color: #$dividerColor;")
                    .replace("--title-color: {COLOR}", "--title-color: #$textColor;")
                    .replace("--subtitle-color: {COLOR}", "--subtitle-color: #$subtitleColor;")
            }
            body {
                val repeatableElement = findId("repeated").removeElement()
                id("content") {
                    downloads.forEach { (download, title) ->
                        appendChild(repeatableElement.clone {
                            tag("a") { attr("href", title) }
                            id("title") { text(createFileTitle(download)) }
                            id("url") { text(download.url) }
                        })
                    }
                }
            }
        }
        val page = createDownloadsPageFile()
        FileWriter(page, false).use { it.write(content) }

        "$FILE$page"
    }

    private suspend fun createDownloadsPageFile(): File {
        val generatedHtml = generatedHtmlDir.file.await()
        generatedHtml.mkdirs()
        return File(generatedHtml, FILENAME)
    }

    private suspend fun createFileUrl(fileName: String): String =
        "$FILE${userPreferencesDataStore.downloadDirectory.get()}/$fileName"

    private fun createFileTitle(downloadItem: DownloadEntry): String {
        val contentSize = if (downloadItem.contentSize.isNotBlank()) {
            "[${downloadItem.contentSize}]"
        } else {
            ""
        }

        return "${downloadItem.title} $contentSize"
    }

    companion object {

        const val FILENAME = "downloads.html"

    }

}
