package acr.browser.lightning.adblock.source

import acr.browser.lightning.adblock.HostsFileParser
import acr.browser.lightning.extensions.onIOExceptionResumeNext
import acr.browser.lightning.log.Logger
import acr.browser.lightning.preference.UserPreferences
import io.reactivex.Single
import java.io.File
import java.io.InputStreamReader

/**
 * A [HostsDataSource] that loads hosts from the file found in [UserPreferences].
 *
 * @param logger The logger used to log information about the loading process.
 * @param file The file from which hosts will be loaded. Must have read access to the file.
 */
class FileHostsDataSource constructor(
    private val logger: Logger,
    private val file: File
) : HostsDataSource {

    /**
     * A [Single] that reads through a local hosts file and extracts the domains that should be
     * redirected to localhost (a.k.a. IP address 127.0.0.1). It can handle files that simply have a
     * list of host names to block, or it can handle a full blown hosts file. It will strip out
     * comments, references to the base IP address and just extract the domains to be used.
     *
     * @see HostsDataSource.loadHosts
     */
    override fun loadHosts(): Single<HostsResult> = Single.create<HostsResult> { emitter ->
        val reader = InputStreamReader(file.inputStream())
        val hostsFileParser = HostsFileParser(logger)

        val domains = hostsFileParser.parseInput(reader)

        logger.log(TAG, "Loaded ${domains.size} domains")
        emitter.onSuccess(HostsResult.Success(domains))
    }.onIOExceptionResumeNext { HostsResult.Failure(it) }

    companion object {
        private const val TAG = "FileHostsDataSource"
    }

}
