package acr.browser.lightning.search.suggestions

import acr.browser.lightning.log.NoOpLogger
import acr.browser.lightning.unimplemented
import android.app.Application
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import io.reactivex.Single
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.*

/**
 * Unit tests for [GoogleSuggestionsModel].
 */
class GoogleSuggestionsModelTest {

    private val httpClient = Single.just(OkHttpClient.Builder().build())
    private val requestFactory = object : RequestFactory {
        override fun createSuggestionsRequest(httpUrl: HttpUrl, encoding: String) = unimplemented()
    }
    private val mockConfiguration = mock<Configuration> {
        on { locales } doReturn LocaleList(Locale.US)
    }.apply {
        locale = Locale.US
    }

    private val mockResources = mock<Resources> {
        on { configuration } doReturn mockConfiguration
    }
    private val application = mock<Application> {
        on { getString(any()) } doReturn "test"
        on { resources } doReturn mockResources
    }

    @Test
    fun `verify query url`() {
        val suggestionsModel = GoogleSuggestionsModel(httpClient, requestFactory, application, NoOpLogger())

        (0..100).forEach {
            val result = "https://suggestqueries.google.com/complete/search?output=toolbar&hl=$it&q=$it"

            assertThat(suggestionsModel.createQueryUrl(it.toString(), it.toString())).isEqualTo(result.toHttpUrlOrNull())
        }
    }
}
