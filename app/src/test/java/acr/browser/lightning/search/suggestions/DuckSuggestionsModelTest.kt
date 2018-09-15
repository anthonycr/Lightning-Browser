package acr.browser.lightning.search.suggestions

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
 * Created by anthonycr on 9/15/18.
 */
class DuckSuggestionsModelTest {

    private val httpClient = OkHttpClient.Builder().build()
    private val requestFactory = object : RequestFactory {
        override fun createSuggestionsRequest(httpUrl: HttpUrl, encoding: String) = unimplemented()
    }
    private val application = mock<Application> {
        on { getString(any()) } doReturn "test"
    }

    @Test
    fun `verify query url`() {
        val suggestionsModel = DuckSuggestionsModel(httpClient, requestFactory, application)

        (0..100).forEach {
            val result = "https://duckduckgo.com/ac/?q=$it"

            assertThat(suggestionsModel.createQueryUrl(it.toString(), "null")).isEqualTo(HttpUrl.parse(result))
        }
    }
}
