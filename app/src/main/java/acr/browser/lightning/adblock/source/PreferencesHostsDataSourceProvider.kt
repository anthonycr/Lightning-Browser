package acr.browser.lightning.adblock.source

import acr.browser.lightning.log.Logger
import acr.browser.lightning.preference.UserPreferences
import android.content.res.AssetManager
import javax.inject.Inject

/**
 * A [HostsDataSourceProvider] backed by [UserPreferences].
 */
class PreferencesHostsDataSourceProvider @Inject constructor(
    private val userPreferences: UserPreferences,
    private val assetManager: AssetManager,
    private val logger: Logger
) : HostsDataSourceProvider {

    override fun createHostsDataSource(): HostsDataSource =
        when (val source = userPreferences.selectedHostsSource()) {
            HostsSourceType.Default -> AssetsHostsDataSource(assetManager, logger)
            is HostsSourceType.Local -> FileHostsDataSource(logger, source.file)
            is HostsSourceType.Remote -> TODO()
        }

    override fun sourceIdentity(): String {
        return userPreferences.selectedHostsSource().identity()
    }

}
