package acr.browser.lightning.adblock.source

import acr.browser.lightning.di.GeneralClient
import acr.browser.lightning.log.Logger
import acr.browser.lightning.preference.UserPreferences
import android.content.res.AssetManager
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * A [HostsDataSourceProvider] backed by [UserPreferences].
 */
class PreferencesHostsDataSourceProvider @Inject constructor(
    private val userPreferences: UserPreferences,
    private val assetManager: AssetManager,
    private val logger: Logger,
    @GeneralClient private val okHttpClient: OkHttpClient
) : HostsDataSourceProvider {

    override fun createHostsDataSource(): HostsDataSource =
        when (val source = userPreferences.selectedHostsSource()) {
            HostsSourceType.Default -> AssetsHostsDataSource(assetManager, logger)
            is HostsSourceType.Local -> FileHostsDataSource(logger, source.file)
            is HostsSourceType.Remote -> UrlHostsDataSource(source.httpUrl, okHttpClient, logger)
        }

    override fun sourceIdentity(): String {
        return userPreferences.selectedHostsSource().identity()
    }

}
