package acr.browser.lightning.adblock

import acr.browser.lightning.adblock.source.HostsDataSourceProvider
import acr.browser.lightning.adblock.util.BloomFilter
import acr.browser.lightning.adblock.util.DefaultBloomFilter
import acr.browser.lightning.adblock.util.DelegatingBloomFilter
import acr.browser.lightning.adblock.util.`object`.JvmObjectStore
import acr.browser.lightning.adblock.util.`object`.ObjectStore
import acr.browser.lightning.adblock.util.hash.MurmurHashStringAdapter
import acr.browser.lightning.database.adblock.Host
import acr.browser.lightning.database.adblock.HostsRepository
import acr.browser.lightning.database.adblock.HostsRepositoryInfo
import acr.browser.lightning.di.DatabaseScheduler
import acr.browser.lightning.log.Logger
import android.app.Application
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Scheduler
import io.reactivex.Single
import java.net.URI
import java.net.URISyntaxException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * An [AdBlocker] that is backed by a [BloomFilter].
 *
 * @param logger The logger used to log status.
 * @param hostsDataSourceProvider The provider that provides the data source used to populate the
 * bloom filter and [hostsRepository].
 * @param hostsRepository The long term store for blocked hosts.
 * @param databaseScheduler The scheduler used to communicate with the database asynchronously.
 */
@Singleton
class BloomFilterAdBlocker @Inject constructor(
    private val logger: Logger,
    hostsDataSourceProvider: HostsDataSourceProvider,
    private val hostsRepository: HostsRepository,
    private val hostsRepositoryInfo: HostsRepositoryInfo,
    application: Application,
    @DatabaseScheduler private val databaseScheduler: Scheduler
) : AdBlocker {

    private val bloomFilter: DelegatingBloomFilter<String> = DelegatingBloomFilter()
    private val objectStore: ObjectStore<DefaultBloomFilter<String>> = JvmObjectStore(application, MurmurHashStringAdapter())

    init {
        Completable.defer {
            if (hostsRepositoryInfo.identity != hostsDataSourceProvider.sourceIdentity()) {
                logger.log(TAG, "New source detected, removing old hosts")
                // TODO don't clear old hosts until we've successfully loaded from the new source.
                hostsRepository.removeAllHosts()
                    .andThen(deleteStoredBloomFilter())
            } else {
                Completable.complete()
            }
        }.andThen(Single.defer {
            if (hostsRepository.hasHosts()) {
                loadStoredBloomFilter()
                    .switchIfEmpty(hostsRepository.allHosts().flatMap(::createAndSaveBloomFilter))
            } else {
                hostsDataSourceProvider.createHostsDataSource()
                    .loadHosts()
                    .map { it.map(::Host) }
                    .flatMap { hostsRepository.addHosts(it).andThen(createAndSaveBloomFilter(it)) }
            }
        }).subscribeOn(databaseScheduler)
            .doOnSuccess {
                bloomFilter.delegate = it
                logger.log(TAG, "Finished loading bloom filter")
            }
            .subscribe()
    }

    private fun loadStoredBloomFilter(): Maybe<BloomFilter<String>> = Maybe.fromCallable {
        objectStore.retrieve(BLOOM_FILTER_KEY)
    }

    private fun deleteStoredBloomFilter(): Completable = Completable.fromAction {
        objectStore.clear(BLOOM_FILTER_KEY)
    }

    private fun createAndSaveBloomFilter(hosts: List<Host>): Single<BloomFilter<String>> = Single.fromCallable {
        logger.log(TAG, "Constructing bloom filter from list")

        val bloomFilter = DefaultBloomFilter(
            numberOfElements = hosts.size,
            falsePositiveRate = 0.01,
            hashingAlgorithm = MurmurHashStringAdapter()
        )
        for (host in hosts) {
            bloomFilter.put(host.name)
        }
        objectStore.store(BLOOM_FILTER_KEY, bloomFilter)

        bloomFilter
    }

    override fun isAd(url: String): Boolean {
        val domain = try {
            getDomainName(url)
        } catch (exception: URISyntaxException) {
            logger.log(TAG, "URL '$url' is invalid", exception)
            return false
        }

        val mightBeOnBlockList = bloomFilter.mightContain(domain.name)

        return if (mightBeOnBlockList) {
            val isOnBlockList = hostsRepository.containsHost(domain)
            if (isOnBlockList) {
                logger.log(TAG, "URL '$url' is an ad")
            } else {
                logger.log(TAG, "False positive for $url")
            }

            isOnBlockList
        } else {
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
        private const val BLOOM_FILTER_KEY = "AdBlockingBloomFilter"
    }

}
