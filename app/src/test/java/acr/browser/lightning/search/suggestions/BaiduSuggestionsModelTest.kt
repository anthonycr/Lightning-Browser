package acr.browser.lightning.search.suggestions

import acr.browser.lightning.log.NoOpLogger
import acr.browser.lightning.unimplemented
import android.app.Application
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Unit tests for [BaiduSuggestionsModel].
 */
class BaiduSuggestionsModelTest {

    private val httpClient = OkHttpClient.Builder().build()
    private val requestFactory = object : RequestFactory {
        override fun createSuggestionsRequest(httpUrl: HttpUrl, encoding: String) = unimplemented()
    }
    private val application = mock<Application> {
        on { getString(any()) } doReturn "test"
    }

    @Test
    fun `verify query url`() {
        val suggestionsModel = BaiduSuggestionsModel(httpClient, requestFactory, application, NoOpLogger())

        (0..100).forEach {
            val result = "http://suggestion.baidu.com/s?wd=$it&action=opensearch"

            assertThat(suggestionsModel.createQueryUrl(it.toString(), "null")).isEqualTo(HttpUrl.parse(result))
        }
    }
}
