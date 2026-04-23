package acr.browser.lightning.adblock.source

import acr.browser.lightning.adblock.parser.HostsFileParser
import acr.browser.lightning.adblock.util.hash.computeMD5
import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.log.Logger
import acr.browser.lightning.preference.UserPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

/**
 * A [HostsDataSource] that loads hosts from the file found in [UserPreferences].
 *
 * @param logger The logger used to log information about the loading process.
 * @param file The file from which hosts will be loaded. Must have read access to the file.
 */
class FileHostsDataSource @AssistedInject constructor(
    private val logger: Logger,
    @Assisted private val file: File,
    private val coroutineDispatchers: CoroutineDispatchers,
) : HostsDataSource {

    /**
     * A [Single] that reads through a local hosts file and extracts the domains that should be
     * redirected to localhost (a.k.a. IP address 127.0.0.1). It can handle files that simply have a
     * list of host names to block, or it can handle a full blown hosts file. It will strip out
     * comments, references to the base IP address and just extract the domains to be used.
     *
     * @see HostsDataSource.loadHosts
     */
    override suspend fun loadHosts(): HostsResult = withContext(coroutineDispatchers.io) {
        try {
            val reader = InputStreamReader(file.inputStream())
            val hostsFileParser = HostsFileParser(logger)

            val domains = hostsFileParser.parseInput(reader)

            logger.log(TAG, "Loaded ${domains.size} domains")

            HostsResult.Success(domains)
        } catch (exception: IOException) {
            HostsResult.Failure(exception)
        }
    }

    override suspend fun identifier(): String = withContext(coroutineDispatchers.io) {
        file.inputStream().computeMD5()
    }

    companion object {
        private const val TAG = "FileHostsDataSource"
    }

    /**
     * The factory used to construct the data source.
     */
    @AssistedFactory
    interface Factory {
        /**
         * Create the data source for the provided file.
         */
        fun create(file: File): FileHostsDataSource
    }

}
