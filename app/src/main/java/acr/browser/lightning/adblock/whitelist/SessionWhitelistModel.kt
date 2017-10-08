package acr.browser.lightning.adblock.whitelist

import acr.browser.lightning.database.whitelist.AdBlockWhitelistModel
import acr.browser.lightning.database.whitelist.WhitelistItem
import acr.browser.lightning.rx.IoSchedulers
import acr.browser.lightning.utils.Utils
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * An in memory representation of the ad blocking whitelist. Can be queried synchronously.
 */
@Singleton
class SessionWhitelistModel @Inject constructor(
        private val adBlockWhitelistModel: AdBlockWhitelistModel
) : WhitelistModel {

    private var whitelistSet = hashSetOf<String>()

    init {
        adBlockWhitelistModel
                .allWhitelistItems()
                .subscribeOn(IoSchedulers.database)
                .subscribe { list ->
                    whitelistSet = list.map(WhitelistItem::url).toHashSet()
                }
    }

    override fun isUrlWhitelisted(url: String): Boolean = whitelistSet.contains(Utils.getDomainName(url))

    override fun addUrlToWhitelist(url: String) {
        Utils.getDomainName(url)?.let { domain ->
            val whitelistItem = WhitelistItem(domain, System.currentTimeMillis())
            adBlockWhitelistModel
                    .whitelistItemForUrl(domain)
                    .isEmpty
                    .filter { it }
                    .flatMapCompletable { adBlockWhitelistModel.addWhitelistItem(whitelistItem) }
                    .subscribeOn(IoSchedulers.database)
                    .subscribe { Log.d(TAG, "whitelist item added to database") }

            whitelistSet.add(domain)
        }
    }

    override fun removeUrlFromWhitelist(url: String) {
        Utils.getDomainName(url)?.let { domain ->
            adBlockWhitelistModel.whitelistItemForUrl(domain)
                    .flatMapCompletable(adBlockWhitelistModel::removeWhitelistItem)
                    .subscribeOn(IoSchedulers.database)
                    .subscribe { Log.d(TAG, "whitelist item removed from database") }

            whitelistSet.remove(domain)
        }
    }

    companion object {
        private const val TAG = "SessionWhitelistModel"
    }
}