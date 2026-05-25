package acr.browser.lightning.adblock.allowlist

import acr.browser.lightning.concurrency.AppCoroutineScope
import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.database.allowlist.AdBlockAllowListRepository
import acr.browser.lightning.database.allowlist.AllowListEntry
import acr.browser.lightning.log.Logger
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * An in memory representation of the ad blocking whitelist. Can be queried synchronously.
 */
@Singleton
class SessionAllowListModel @Inject constructor(
    private val adBlockAllowListModel: AdBlockAllowListRepository,
    private val logger: Logger,
    private val appCoroutineScope: AppCoroutineScope,
    private val coroutineDispatchers: CoroutineDispatchers,
) : AllowListModel {

    private var whitelistSet = hashSetOf<String>()

    init {
        appCoroutineScope.launch(coroutineDispatchers.default) {
            whitelistSet = adBlockAllowListModel.allAllowListItems()
                .map(AllowListEntry::domain)
                .toHashSet()
        }
    }

    override fun isUrlAllowedAds(url: String): Boolean =
        url.toUri().host?.let(whitelistSet::contains) ?: false

    override fun addUrlToAllowList(url: String) {
        url.toUri().host?.let { host ->
            appCoroutineScope.launch(coroutineDispatchers.default) {
                val item = adBlockAllowListModel.allowListItemForUrl(host)
                if (item == null) {
                    adBlockAllowListModel.addAllowListItem(
                        AllowListEntry(host, System.currentTimeMillis())
                    )
                }
                logger.log(TAG, "whitelist item added to database")
            }

            whitelistSet.add(host)
        }
    }

    override fun removeUrlFromAllowList(url: String) {
        url.toUri().host?.let { host ->
            appCoroutineScope.launch(coroutineDispatchers.default) {
                val item = adBlockAllowListModel.allowListItemForUrl(host) ?: return@launch
                adBlockAllowListModel.removeAllowListItem(item)
                logger.log(TAG, "whitelist item removed from database")
            }

            whitelistSet.remove(host)
        }
    }

    companion object {
        private const val TAG = "SessionAllowListModel"
    }
}
