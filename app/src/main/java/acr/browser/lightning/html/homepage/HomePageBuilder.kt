package acr.browser.lightning.html.homepage

import acr.browser.lightning.R
import acr.browser.lightning.constant.UTF8
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
            outputSettings().charset(UTF8)
        }

        val currentSearchEngine = searchEngineProvider.getCurrentSearchEngine()

        val iconUrl = currentSearchEngine.iconUrl
        val searchUrl = currentSearchEngine.queryUrl

        val body = document.body()

        body.getElementById("image_url").attr("src", iconUrl)

        document.getElementsByTag("script").firstOrNull()?.let {
            val newJavaScript = it.html().replace("\${BASE_URL}", searchUrl).replace("&", "\\u0026")
            it.html(newJavaScript)
        }

        return document.outerHtml()
    }
}