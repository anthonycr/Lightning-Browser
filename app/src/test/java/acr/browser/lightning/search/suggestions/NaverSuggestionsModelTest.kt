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
 * Unit tests for [NaverSuggestionsModel].
 */
class NaverSuggestionsModelTest {

    private val httpClient = OkHttpClient.Builder().build()
    private val requestFactory = object : RequestFactory {
        override fun createSuggestionsRequest(httpUrl: HttpUrl, encoding: String) = unimplemented()
    }
    private val application = mock<Application> {
        on { getString(any()) } doReturn "test"
    }

    @Test
    fun `verify query url`() {
        val model = NaverSuggestionsModel(httpClient, requestFactory, application, NoOpLogger())

        (0..100).forEach {
            val result = "https://ac.search.naver.com/nx/ac?q=$it&q_enc=UTF-8&st=100&frm=nv&r_format=json&r_enc=UTF-8&r_unicode=0&t_koreng=1&ans=2&run=2&rev=4&con=1"

            assertThat(model.createQueryUrl(it.toString(), "null")).isEqualTo(HttpUrl.parse(result))
        }
    }
}
