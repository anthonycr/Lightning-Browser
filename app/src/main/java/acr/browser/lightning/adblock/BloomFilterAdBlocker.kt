package acr.browser.lightning.adblock

import acr.browser.lightning.database.adblock.Host
import acr.browser.lightning.database.adblock.HostsRepository
import acr.browser.lightning.di.DatabaseScheduler
import acr.browser.lightning.log.Logger
import com.google.common.hash.BloomFilter
import com.google.common.hash.Funnels
import io.reactivex.Scheduler
import io.reactivex.rxkotlin.toObservable
import java.net.URI
import java.net.URISyntaxException
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * An [AdBlocker] that is backed by a [BloomFilter].
 */
@Singleton
class BloomFilterAdBlocker @Inject constructor(
    private val logger: Logger,
    hostsDataSource: HostsDataSource,
    private val hostsRepository: HostsRepository,
    @DatabaseScheduler private val databaseScheduler: Scheduler
) : AdBlocker {

    private val bloomFilter = BloomFilter.create(
        Funnels.stringFunnel(Charset.defaultCharset()),
        50_000,
        0.01
    )

    init {
        hostsRepository
            .removeAllHosts()
            .andThen(hostsDataSource.loadHosts())
            .flatMapObservable { it.toObservable() }
            .doOnNext { bloomFilter.put(it) }
            .map(::Host)
            .toList()
            .flatMapCompletable(hostsRepository::addHosts)
            .subscribeOn(databaseScheduler)
            .doOnComplete {
                logger.log(TAG, "Finished loading bloom filter")
            }
            .subscribe()
    }

    override fun isAd(url: String?): Boolean {
        if (url == null) {
            return false
        }

        val domain = try {
            getDomainName(url)
        } catch (exception: URISyntaxException) {
            logger.log(TAG, "URL '$url' is invalid", exception)
            return false
        }

        val mightBeOnBlockList = bloomFilter.mightContain(domain.name)

        return if (mightBeOnBlockList) {
            val isOnBlockList = hostsRepository.containsHost(domain).blockingGet()
            if (isOnBlockList) {
                logger.log(TAG, "URL '$url' is an ad")
            } else {
                logger.log(TAG, "False positive for $url")
            }

            isOnBlockList
        } else {
            if (hostsRepository.containsHost(domain).blockingGet()) {
                logger.log(TAG, "URL SHOULD BE AN AD: $url")
            }
            false
        }
    }

    /**
     * Returns the probable domain name for a given URL
     *
     * @param url the url to parse
     * @return returns the domain
     * @throws URISyntaxException throws an exception if the string cannot form a URI
     */
    @Throws(URISyntaxException::class)
    private fun getDomainName(url: String): Host {
        val host = url.indexOf('/', 8)
            .takeIf { it != -1 }
            ?.let(url::take)
            ?: url

        val uri = URI(host)
        val domain = uri.host ?: return Host(host)

        return Host(if (domain.startsWith("www.")) {
            domain.substring(4)
        } else {
            domain
        })
    }

    companion object {
        private const val TAG = "BloomFilterAdBlocker"
    }

}
