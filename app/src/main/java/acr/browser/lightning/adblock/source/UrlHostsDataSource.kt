package acr.browser.lightning.adblock.source

import acr.browser.lightning.adblock.HostsFileParser
import acr.browser.lightning.extensions.onIOExceptionResumeNext
import acr.browser.lightning.log.Logger
import io.reactivex.Single
import okhttp3.*
import java.io.IOException
import java.io.InputStreamReader

/**
 * A [HostsDataSource] that loads hosts from an [HttpUrl].
 */
class UrlHostsDataSource(
    private val url: HttpUrl,
    private val okHttpClient: OkHttpClient,
    private val logger: Logger
) : HostsDataSource {

    override fun loadHosts(): Single<HostsResult> = Single.create<HostsResult> { emitter ->
        val request = Request.Builder().url(url).get().build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                emitter.onError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                val input = response.body()?.byteStream()?.let(::InputStreamReader)
                    ?: return emitter.onError(IOException("Empty response"))

                val hostsFileParser = HostsFileParser(logger)

                val domains = hostsFileParser.parseInput(input)

                logger.log(TAG, "Loaded ${domains.size} domains")
                emitter.onSuccess(HostsResult.Success(domains))
            }
        })
    }.onIOExceptionResumeNext { HostsResult.Failure(it) }

    override fun identifier(): String = url.toString()

    override fun requiresRefresh(): Boolean {
        // TODO setup refresh sync
        return false
    }

    companion object {
        private const val TAG = "UrlHostsDataSource"
    }

}
