package acr.browser.lightning.html.history

import acr.browser.lightning.R
import acr.browser.lightning.browser.di.GeneratedHtmlDir
import acr.browser.lightning.browser.di.IncognitoMode
import acr.browser.lightning.compose.asColorScheme
import acr.browser.lightning.compose.toRgbHexString
import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.constant.FILE
import acr.browser.lightning.database.history.HistoryRepository
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
 * Factory for the history page.
 */
class HistoryPageFactory @Inject constructor(
    private val listPageReader: ListPageReader,
    application: Application,
    private val historyRepository: HistoryRepository,
    @IncognitoMode private val isIncognito: Boolean,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val coroutineDispatchers: CoroutineDispatchers,
    @GeneratedHtmlDir private val generatedHtmlDir: ThreadSafeFileProvider,
) : HtmlPageFactory {

    private val title = application.getString(R.string.action_history)

    override suspend fun buildPage(): String = withContext(coroutineDispatchers.io) {
        val appTheme = userPreferencesDataStore.useTheme.get()
        val colorScheme = appTheme.asColorScheme(isIncognito)
        val list = historyRepository.lastHundredVisitedHistoryEntries()
        val content = parse(listPageReader.provideHtml()) andBuild {
            title { title }
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
                val repeatedElement = findId("repeated").removeElement()
                id("content") {
                    list.forEach {
                        appendChild(repeatedElement.clone {
                            tag("a") { attr("href", it.url) }
                            id("title") { text(it.title) }
                            id("url") { text(it.url) }
                        })
                    }
                }
            }
        }

        val page = createHistoryPage()
        FileWriter(page, false).use { it.write(content) }

        "$FILE$page"
    }

    /**
     * Use this observable to immediately delete the history page. This will clear the cached
     * history page that was stored on file.
     *
     * @return a completable that deletes the history page when subscribed to.
     */
    suspend fun deleteHistoryPage(): Unit = withContext(coroutineDispatchers.io) {
        with(createHistoryPage()) {
            if (exists()) {
                delete()
            }
        }
    }

    private suspend fun createHistoryPage(): File {
        val generatedHtml = generatedHtmlDir.file()
        generatedHtml.mkdirs()
        return File(generatedHtml, FILENAME)
    }

    companion object {
        const val FILENAME = "history.html"
    }

}
