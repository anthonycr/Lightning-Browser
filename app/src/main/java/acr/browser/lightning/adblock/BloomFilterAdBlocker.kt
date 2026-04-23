package acr.browser.lightning.adblock

import acr.browser.lightning.R
import acr.browser.lightning.adblock.source.HostsDataSourceProvider
import acr.browser.lightning.adblock.source.HostsResult
import acr.browser.lightning.adblock.util.BloomFilter
import acr.browser.lightning.adblock.util.DefaultBloomFilter
import acr.browser.lightning.adblock.util.DelegatingBloomFilter
import acr.browser.lightning.adblock.util.hash.MurmurHashHostAdapter
import acr.browser.lightning.adblock.util.hash.MurmurHashStringAdapter
import acr.browser.lightning.adblock.util.`object`.JvmObjectStore
import acr.browser.lightning.adblock.util.`object`.ObjectStore
import acr.browser.lightning.browser.di.DatabaseScheduler
import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.database.adblock.Host
import acr.browser.lightning.database.adblock.HostsPreferenceStore
import acr.browser.lightning.database.adblock.HostsRepository
import acr.browser.lightning.extensions.toast
import acr.browser.lightning.log.Logger
import android.app.Application
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
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
 */
@Singleton
class BloomFilterAdBlocker @Inject constructor(
    private val logger: Logger,
    private val hostsDataSourceProvider: HostsDataSourceProvider,
    private val hostsRepository: HostsRepository,
    private val hostsPreferenceStore: HostsPreferenceStore,
    private val application: Application,
    private val appCoroutineScope: CoroutineScope,
    @DatabaseScheduler
    private val objectStoreDispatcher: CoroutineDispatcher,
    private val coroutineDispatchers: CoroutineDispatchers,
) : AdBlocker {

    private val bloomFilter: DelegatingBloomFilter<Host> = DelegatingBloomFilter()
    private val objectStore: ObjectStore<DefaultBloomFilter<Host>> = JvmObjectStore(
        application = application,
        hashingAlgorithm = MurmurHashStringAdapter(),
        key = BLOOM_FILTER_KEY,
        objectStoreDispatcher = objectStoreDispatcher,
    )

    private val loadHostsFlow = MutableStateFlow(true)

    init {
        loadHostsFlow
            .buffer(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
            .map { forceRefresh ->
                val storedBloomFilter = objectStore.retrieve()
                val hostsDataSource = hostsDataSourceProvider.createHostsDataSource()
                val hostsDataSourceIdentifier = hostsDataSource.identifier()
                // Force a new hosts request if the hosts are out of date or if the repo has no hosts.
                if (!forceRefresh &&
                    storedBloomFilter != null &&
                    hostsRepository.hasHosts() &&
                    hostsPreferenceStore.identity.get() == hostsDataSourceIdentifier
                ) {
                    return@map storedBloomFilter
                }

                when (val result = hostsDataSource.loadHosts()) {
                    is HostsResult.Failure -> {
                        logger.log(TAG, "Unable to load hosts", result.cause)
                        null
                    }

                    is HostsResult.Success -> {
                        // Clear out the old hosts and bloom filter now that we have the new hosts.
                        hostsRepository.removeAllHosts()
                        hostsRepository.addHosts(result.hosts)
                        hostsPreferenceStore.identity.set(hostsDataSourceIdentifier)
                        createAndSaveBloomFilter(result.hosts)
                    }
                }
            }
            .flowOn(coroutineDispatchers.io)
            .onEach {
                // If we were unsuccessful in loading hosts, and we don't have hosts in the repo, don't
                // allow initialization, as false positives will result in bad browsing experience.
                if (hostsRepository.hasHosts() && it != null) {
                    bloomFilter.delegate = it
                    logger.log(TAG, "Finished loading bloom filter")
                } else {
                    logger.log(TAG, "Failed to load bloom filter")
                    appCoroutineScope.launch(coroutineDispatchers.main) {
                        application.toast(R.string.ad_block_load_failure)
                    }
                }
            }
            .launchIn(appCoroutineScope)
    }

    /**
     * Force the ad blocker to (re)populate its internal hosts filter from the provided hosts data
     * source.
     */
    fun populateAdBlockerFromDataSource(forceRefresh: Boolean) = loadHostsFlow.tryEmit(forceRefresh)

    private suspend fun createAndSaveBloomFilter(hosts: List<Host>): BloomFilter<Host> {
        logger.log(TAG, "Constructing bloom filter from list")

        val bloomFilter = DefaultBloomFilter(
            numberOfElements = hosts.size,
            falsePositiveRate = 0.01,
            hashingAlgorithm = MurmurHashHostAdapter()
        )
        bloomFilter.putAll(hosts)
        objectStore.store(bloomFilter)

        return bloomFilter
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
        this.toUri().host?.let(::Host)
    } catch (exception: URISyntaxException) {
        logger.log(TAG, "Invalid URL: $this", exception)
        null
    }

    companion object {
        private const val TAG = "BloomFilterAdBlocker"
        private const val BLOOM_FILTER_KEY = "AdBlockingBloomFilter"
    }
}
