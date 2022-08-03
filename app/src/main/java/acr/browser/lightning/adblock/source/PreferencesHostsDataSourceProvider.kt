package acr.browser.lightning.adblock.source

import acr.browser.lightning.browser.di.HostsClient
import acr.browser.lightning.log.Logger
import acr.browser.lightning.preference.UserPreferences
import android.app.Application
import android.content.res.AssetManager
import dagger.Reusable
import io.reactivex.Single
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * A [HostsDataSourceProvider] backed by [UserPreferences].
 */
@Reusable
class PreferencesHostsDataSourceProvider @Inject constructor(
    private val userPreferences: UserPreferences,
    private val assetManager: AssetManager,
    private val logger: Logger,
    @HostsClient private val okHttpClient: Single<OkHttpClient>,
    private val application: Application
) : HostsDataSourceProvider {

    override fun createHostsDataSource(): HostsDataSource =
        when (val source = userPreferences.selectedHostsSource()) {
            HostsSourceType.Default -> AssetsHostsDataSource(assetManager, logger)
            is HostsSourceType.Local -> FileHostsDataSource(logger, source.file)
            is HostsSourceType.Remote -> UrlHostsDataSource(source.httpUrl, okHttpClient, logger, userPreferences, application)
        }

}
