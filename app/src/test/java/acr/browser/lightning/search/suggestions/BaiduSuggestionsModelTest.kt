package acr.browser.lightning.search.suggestions

import acr.browser.lightning.concurrency.FakeCoroutineDispatchers
import acr.browser.lightning.log.NoOpLogger
import acr.browser.lightning.resources.FakeResourceProvider
import acr.browser.lightning.unimplemented
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.Locale

/**
 * Unit tests for [BaiduSuggestionsModel].
 */
class BaiduSuggestionsModelTest {

    private val httpClient = CompletableDeferred(OkHttpClient.Builder().build())
    private val requestFactory = object : RequestFactory {
        override fun createSuggestionsRequest(httpUrl: HttpUrl, encoding: String) = unimplemented()
    }

    @Test
    fun `verify query url`() = runTest {
        val suggestionsModel = BaiduSuggestionsModel(
            httpClient,
            requestFactory,
            Locale.ROOT,
            FakeResourceProvider(),
            NoOpLogger(),
            FakeCoroutineDispatchers(testScheduler)
        )

        (0..100).forEach {
            val result = "http://suggestion.baidu.com/su?wd=$it&json=2&cb="

            assertThat(
                suggestionsModel.createQueryUrl(
                    it.toString(),
                    "null"
                )
            ).isEqualTo(result.toHttpUrlOrNull())
        }
    }
}
