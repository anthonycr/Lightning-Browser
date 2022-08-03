package acr.browser.lightning.adblock

import acr.browser.lightning.R
import acr.browser.lightning.adblock.source.HostsDataSourceProvider
import acr.browser.lightning.adblock.source.HostsResult
import acr.browser.lightning.adblock.util.BloomFilter
import acr.browser.lightning.adblock.util.DefaultBloomFilter
import acr.browser.lightning.adblock.util.DelegatingBloomFilter
import acr.browser.lightning.adblock.util.`object`.JvmObjectStore
import acr.browser.lightning.adblock.util.`object`.ObjectStore
import acr.browser.lightning.adblock.util.hash.MurmurHashHostAdapter
import acr.browser.lightning.adblock.util.hash.MurmurHashStringAdapter
import acr.browser.lightning.database.adblock.Host
import acr.browser.lightning.database.adblock.HostsRepository
import acr.browser.lightning.database.adblock.HostsRepositoryInfo
import acr.browser.lightning.browser.di.DatabaseScheduler
import acr.browser.lightning.browser.di.MainScheduler
import acr.browser.lightning.extensions.toast
import acr.browser.lightning.log.Logger
import android.app.Application
import android.net.Uri
import io.reactivex.Maybe
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
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
    private val hostsDataSourceProvider: HostsDataSourceProvider,
    private val hostsRepository: HostsRepository,
    private val hostsRepositoryInfo: HostsRepositoryInfo,
    private val application: Application,
    @DatabaseScheduler private val databaseScheduler: Scheduler,
    @MainScheduler private val mainScheduler: Scheduler
) : AdBlocker {

    private val bloomFilter: DelegatingBloomFilter<Host> = DelegatingBloomFilter()
    private val objectStore: ObjectStore<DefaultBloomFilter<Host>> = JvmObjectStore(application, MurmurHashStringAdapter())

    private val compositeDisposable = CompositeDisposable()

    init {
        populateAdBlockerFromDataSource(forceRefresh = false)
    }

    /**
     * Force the ad blocker to (re)populate its internal hosts filter from the provided hosts data
     * source.
     */
    fun populateAdBlockerFromDataSource(forceRefresh: Boolean) {
        compositeDisposable.clear()
        compositeDisposable += Single.fromCallable(hostsDataSourceProvider::createHostsDataSource)
            .flatMapMaybe { hostsDataSource ->
                loadStoredBloomFilter().filter {
                    // Force a new hosts request if the hosts are out of date or if the repo has no hosts.
                    hostsRepositoryInfo.identity == hostsDataSource.identifier()
                        && hostsRepository.hasHosts()
                        && !forceRefresh
                }.switchIfEmpty(
                    hostsDataSourceProvider
                        .createHostsDataSource()
                        .loadHosts()
                        .flatMapMaybe {
                            when (it) {
                                is HostsResult.Success -> Maybe.just(it.hosts)
                                is HostsResult.Failure -> Maybe.empty<List<Host>>().doOnComplete {
                                    logger.log(TAG, "Unable to load hosts", it.cause)
                                }
                            }
                        }
                        .flatMapSingleElement {
                            logger.log(TAG, "Loaded ${it.size} hosts")
                            // Clear out the old hosts and bloom filter now that we have the new hosts.
                            hostsRepository.removeAllHosts()
                                .andThen(hostsRepository.addHosts(it))
                                .andThen(createAndSaveBloomFilter(it))
                                .doOnSuccess {
                                    hostsRepositoryInfo.identity = hostsDataSource.identifier()
                                }
                        }
                )
            }
            .filter {
                // If we were unsuccessful in loading hosts and we don't have hosts in the repo, don't
                // allow initialization, as false positives will result in bad browsing experience.
                hostsRepository.hasHosts()
            }.subscribeOn(databaseScheduler)
            .observeOn(mainScheduler)
            .subscribeBy(
                onSuccess = {
                    bloomFilter.delegate = it
                    logger.log(TAG, "Finished loading bloom filter")
                },
                onComplete = {
                    application.toast(R.string.ad_block_load_failure)
                }
            )
    }

    private fun loadStoredBloomFilter(): Maybe<BloomFilter<Host>> = Maybe.fromCallable {
        objectStore.retrieve(BLOOM_FILTER_KEY)
    }

    private fun createAndSaveBloomFilter(hosts: List<Host>): Single<BloomFilter<Host>> = Single.fromCallable {
        logger.log(TAG, "Constructing bloom filter from list")

        val bloomFilter = DefaultBloomFilter(
            numberOfElements = hosts.size,
            falsePositiveRate = 0.01,
            hashingAlgorithm = MurmurHashHostAdapter()
        )
        bloomFilter.putAll(hosts)
        objectStore.store(BLOOM_FILTER_KEY, bloomFilter)

        bloomFilter
    }

    override fun isAd(url: String): Boolean {
        val domain = url.host() ?: return false

        val mightBeOnBlockList = bloomFilter.mightContain(domain)

        return when {
            mightBeOnBlockList -> {
                val isOnBlockList = hostsRepository.containsHost(domain)
                if (isOnBlockList) {
                    logger.log(TAG, "URL '$url' is an ad")
                } else {
                    logger.log(TAG, "False positive for $url")
                }

                isOnBlockList
            }
            domain.name.startsWith("www.") -> isAd(domain.name.substring(4))
            else -> false
        }
    }

    /**
     * Extract the [Host] from a [String] representing a URL. Returns null if no host was extracted.
     */
    private fun String.host(): Host? = try {
        Uri.parse(this).host?.let(::Host)
    } catch (exception: URISyntaxException) {
        logger.log(TAG, "Invalid URL: $this", exception)
        null
    }

    companion object {
        private const val TAG = "BloomFilterAdBlocker"
        private const val BLOOM_FILTER_KEY = "AdBlockingBloomFilter"
    }

}
