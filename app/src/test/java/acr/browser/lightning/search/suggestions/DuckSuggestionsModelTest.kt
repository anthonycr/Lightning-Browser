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
 * Created by anthonycr on 9/15/18.
 */
class DuckSuggestionsModelTest {

    private val httpClient = CompletableDeferred(OkHttpClient.Builder().build())
    private val requestFactory = object : RequestFactory {
        override fun createSuggestionsRequest(httpUrl: HttpUrl, encoding: String) = unimplemented()
    }

    @Test
    fun `verify query url`() = runTest {
        val suggestionsModel = DuckSuggestionsModel(
            httpClient,
            requestFactory,
            Locale.ROOT,
            FakeResourceProvider(),
            NoOpLogger(),
            FakeCoroutineDispatchers(testScheduler)
        )

        (0..100).forEach {
            val result = "https://duckduckgo.com/ac/?q=$it"

            assertThat(
                suggestionsModel.createQueryUrl(it.toString(), "null")
            ).isEqualTo(result.toHttpUrlOrNull())
        }
    }
}
