package acr.browser.lightning.adblock.source

import acr.browser.lightning.preference.UserPreferences
import dagger.Reusable
import javax.inject.Inject

/**
 * A [HostsDataSourceProvider] backed by [UserPreferences].
 */
@Reusable
class PreferencesHostsDataSourceProvider @Inject constructor(
    private val userPreferences: UserPreferences,
    private val assetsHostsDataSource: AssetsHostsDataSource,
    private val fileHostsDataSourceFactory: FileHostsDataSource.Factory,
    private val urlHostsDataSourceFactory: UrlHostsDataSource.Factory
) : HostsDataSourceProvider {

    override fun createHostsDataSource(): HostsDataSource =
        when (val source = userPreferences.selectedHostsSource()) {
            HostsSourceType.Default -> assetsHostsDataSource
            is HostsSourceType.Local -> fileHostsDataSourceFactory.create(source.file)
            is HostsSourceType.Remote -> urlHostsDataSourceFactory.create(source.httpUrl)
        }
}
