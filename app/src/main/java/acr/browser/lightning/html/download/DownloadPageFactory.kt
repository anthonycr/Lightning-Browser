package acr.browser.lightning.html.download

import acr.browser.lightning.R
import acr.browser.lightning.browser.di.GeneratedHtmlDir
import acr.browser.lightning.browser.di.IncognitoMode
import acr.browser.lightning.compose.asColorScheme
import acr.browser.lightning.compose.toRgbHexString
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
    @IncognitoMode private val isIncognito: Boolean,
    private val coroutineDispatchers: CoroutineDispatchers,
    @GeneratedHtmlDir private val generatedHtmlDir: ThreadSafeFileProvider,
) : HtmlPageFactory {

    override suspend fun buildPage(): String = withContext(coroutineDispatchers.io) {
        val appTheme = userPreferencesDataStore.useTheme.get()
        val colorScheme = appTheme.asColorScheme(isIncognito)
        val downloads = manager.getAllDownloads().map { it to createFileUrl(it.title) }
        val content = parse(listPageReader.provideHtml()) andBuild {
            title { application.getString(R.string.action_downloads) }
            style { content ->
                content.replace(
                    "--body-bg: {COLOR}",
                    "--body-bg: #${colorScheme.surface.toRgbHexString()};"
                ).replace(
                    "--divider-color: {COLOR}",
                    "--divider-color: #${colorScheme.outlineVariant.toRgbHexString()};"
                ).replace(
                    "--title-color: {COLOR}",
                    "--title-color: #${colorScheme.onSurface.toRgbHexString()};"
                ).replace(
                    "--subtitle-color: {COLOR}",
                    "--subtitle-color: #${colorScheme.onSurfaceVariant.toRgbHexString()};"
                )
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
        val generatedHtml = generatedHtmlDir.file()
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
