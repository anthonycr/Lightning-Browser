package acr.browser.lightning.adblock.source

import acr.browser.lightning.adblock.parser.HostsFileParser
import acr.browser.lightning.browser.di.HostsClient
import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.log.Logger
import acr.browser.lightning.preference.UserPreferencesDataStore
import acr.browser.lightning.preference.userAgent
import android.app.Application
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.InputStreamReader
import kotlin.coroutines.resume

/**
 * A [HostsDataSource] that loads hosts from an [HttpUrl].
 */
class UrlHostsDataSource @AssistedInject constructor(
    @Assisted private val url: HttpUrl,
    @HostsClient private val okHttpClient: Deferred<@JvmSuppressWildcards OkHttpClient>,
    private val logger: Logger,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val application: Application,
    private val coroutineDispatchers: CoroutineDispatchers,
) : HostsDataSource {

    override suspend fun loadHosts(): HostsResult = withContext(coroutineDispatchers.network) {
        val client = okHttpClient.await()
        suspendCancellableCoroutine { emitter ->
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userPreferencesDataStore.userAgent(application))
                .get()
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    emitter.resume(HostsResult.Failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    val successfulResponse = response.takeIf(Response::isSuccessful)
                        ?: return emitter.resume(HostsResult.Failure(IOException("Error reading remote file")))
                    val input = InputStreamReader(successfulResponse.body.byteStream())

                    val hostsFileParser = HostsFileParser(logger)

                    val domains = hostsFileParser.parseInput(input)

                    logger.log(TAG, "Loaded ${domains.size} domains")
                    emitter.resume(HostsResult.Success(domains))
                }
            })
        }
    }

    override suspend fun identifier(): String = url.toString()

    companion object {
        private const val TAG = "UrlHostsDataSource"
    }

    /**
     * Used to create the data source.
     */
    @AssistedFactory
    interface Factory {
        /**
         * Create the data source for the provided URL.
         */
        fun create(url: HttpUrl): UrlHostsDataSource
    }

}
