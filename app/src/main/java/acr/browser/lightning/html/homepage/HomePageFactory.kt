package acr.browser.lightning.html.homepage

import acr.browser.lightning.R
import acr.browser.lightning.browser.di.GeneratedHtmlDir
import acr.browser.lightning.browser.theme.ThemeProvider
import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.constant.FILE
import acr.browser.lightning.constant.UTF8
import acr.browser.lightning.html.HtmlPageFactory
import acr.browser.lightning.html.jsoup.andBuild
import acr.browser.lightning.html.jsoup.body
import acr.browser.lightning.html.jsoup.charset
import acr.browser.lightning.html.jsoup.id
import acr.browser.lightning.html.jsoup.parse
import acr.browser.lightning.html.jsoup.style
import acr.browser.lightning.html.jsoup.tag
import acr.browser.lightning.html.jsoup.title
import acr.browser.lightning.search.SearchEngineProvider
import acr.browser.lightning.utils.ThreadSafeFileProvider
import android.app.Application
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import javax.inject.Inject

/**
 * A factory for the home page.
 */
class HomePageFactory @Inject constructor(
    application: Application,
    private val searchEngineProvider: SearchEngineProvider,
    private val homePageReader: HomePageReader,
    private val themeProvider: ThemeProvider,
    private val coroutineDispatchers: CoroutineDispatchers,
    @GeneratedHtmlDir private val generatedHtmlDir: ThreadSafeFileProvider,
) : HtmlPageFactory {

    private val title = application.getString(R.string.home)

    private fun Int.toColor(): String {
        val string = Integer.toHexString(this)

        return string.substring(2) + string.substring(0, 2)
    }

    private val backgroundColor: String
        get() = themeProvider.color(R.attr.colorPrimary).toColor()
    private val cardColor: String
        get() = themeProvider.color(R.attr.autoCompleteBackgroundColor).toColor()
    private val textColor: String
        get() = themeProvider.color(R.attr.autoCompleteTitleColor).toColor()

    override suspend fun buildPage(): String = withContext(coroutineDispatchers.io) {
        val (iconUrl, queryUrl, _) = searchEngineProvider.provideSearchEngine()
        val content = parse(homePageReader.provideHtml()) andBuild {
            title { title }
            style { content ->
                content.replace("--body-bg: {COLOR}", "--body-bg: #$backgroundColor;")
                    .replace("--box-bg: {COLOR}", "--box-bg: #$cardColor;")
                    .replace("--box-txt: {COLOR}", "--box-txt: #$textColor;")
            }
            charset { UTF8 }
            body {
                id("image_url") { attr("src", iconUrl) }
                tag("script") {
                    html(
                        html()
                            .replace($$"${BASE_URL}", queryUrl)
                            .replace("&", "\\u0026")
                    )
                }
            }
        }
        val page = createHomePage()
        FileWriter(page, false).use {
            it.write(content)
        }

        "$FILE$page"
    }

    /**
     * Create the home page file.
     */
    private suspend fun createHomePage(): File {
        val generatedHtml = generatedHtmlDir.file.await()
        generatedHtml.mkdirs()
        return File(generatedHtml, FILENAME)
    }

    companion object {

        const val FILENAME = "homepage.html"

    }

}
