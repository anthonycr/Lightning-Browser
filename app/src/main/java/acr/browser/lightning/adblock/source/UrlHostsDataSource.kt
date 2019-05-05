package acr.browser.lightning.adblock.source

import acr.browser.lightning.adblock.HostsFileParser
import acr.browser.lightning.log.Logger
import io.reactivex.Single
import okhttp3.*
import java.io.IOException
import java.io.InputStreamReader
import java.util.*

/**
 * A [HostsDataSource] that loads hosts from an [HttpUrl].
 */
class UrlHostsDataSource(
    private val url: HttpUrl,
    private val okHttpClient: OkHttpClient,
    private val logger: Logger
) : HostsDataSource {

    override fun loadHosts(): Single<List<String>> = Single.create { emitter ->
        // TODO don't emit errors, instead use a Maybe, handle in AdBlocker
        val request = Request.Builder().url(url).get().build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                emitter.onError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                val input = response.body()?.byteStream()?.let(::InputStreamReader)
                    ?: return emitter.onError(IOException("Empty response"))

                val hostsFileParser = HostsFileParser()
                val time = System.currentTimeMillis()

                val domains = ArrayList<String>(1)

                input.use { inputStreamReader ->
                    inputStreamReader.forEachLine {
                        hostsFileParser.parseLine(it, domains)
                    }
                }

                logger.log(TAG, "Loaded ad list in: ${(System.currentTimeMillis() - time)} ms")
                logger.log(TAG, "Loaded ${domains.size} domains")
                emitter.onSuccess(domains)
            }
        })

    }

    companion object {
        private const val TAG = "UrlHostsDataSource"
    }

}
