package acr.browser.lightning.html.homepage

import acr.browser.lightning.R
import acr.browser.lightning.search.SearchEngineProvider
import android.app.Application
import com.anthonycr.mezzanine.MezzanineGenerator
import org.jsoup.Jsoup

/**
 * A builder for the home page.
 */
internal class HomePageBuilder(private val app: Application,
                      private val searchEngineProvider: SearchEngineProvider) {


    fun buildPage(): String {
        val html = MezzanineGenerator.HomePageReader().provideHtml()

        val document = Jsoup.parse(html).apply {
            title(app.getString(R.string.home))
        }

        val currentSearchEngine = searchEngineProvider.getCurrentSearchEngine()

        val iconUrl = currentSearchEngine.iconUrl
        val searchUrl = currentSearchEngine.queryUrl

        val body = document.body()

        body.getElementById("image_url").attr("src", iconUrl)

        val javaScriptTag = document.getElementsByTag("script")
        val javaScript = javaScriptTag.html()
        val newJavaScript = javaScript.replace("\${BASE_URL}", searchUrl)
        javaScriptTag.html(newJavaScript)

        return document.outerHtml()
    }
}