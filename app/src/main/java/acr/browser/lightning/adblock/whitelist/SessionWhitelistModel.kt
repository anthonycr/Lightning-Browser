package acr.browser.lightning.adblock.whitelist

import acr.browser.lightning.database.whitelist.AdBlockWhitelistRepository
import acr.browser.lightning.database.whitelist.WhitelistItem
import android.util.Log
import androidx.core.net.toUri
import io.reactivex.Completable
import io.reactivex.Scheduler
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * An in memory representation of the ad blocking whitelist. Can be queried synchronously.
 */
@Singleton
class SessionWhitelistModel @Inject constructor(
    private val adBlockWhitelistModel: AdBlockWhitelistRepository,
    @Named("database") private val ioScheduler: Scheduler
) : WhitelistModel {

    private var whitelistSet = hashSetOf<String>()

    init {
        adBlockWhitelistModel
            .allWhitelistItems()
            .map { it.map(WhitelistItem::url).toHashSet() }
            .subscribeOn(ioScheduler)
            .subscribe { hashSet -> whitelistSet = hashSet }
    }

    override fun isUrlWhitelisted(url: String): Boolean =
        url.toUri().host?.let(whitelistSet::contains) ?: false

    override fun addUrlToWhitelist(url: String) {
        url.toUri().host?.let { host ->
            adBlockWhitelistModel
                .whitelistItemForUrl(host)
                .isEmpty
                .flatMapCompletable {
                    if (it) {
                        adBlockWhitelistModel.addWhitelistItem(
                            WhitelistItem(host, System.currentTimeMillis())
                        )
                    } else {
                        Completable.complete()
                    }
                }
                .subscribeOn(ioScheduler)
                .subscribe { Log.d(TAG, "whitelist item added to database") }

            whitelistSet.add(host)
        }
    }

    override fun removeUrlFromWhitelist(url: String) {
        url.toUri().host?.let { host ->
            adBlockWhitelistModel
                .whitelistItemForUrl(host)
                .flatMapCompletable(adBlockWhitelistModel::removeWhitelistItem)
                .subscribeOn(ioScheduler)
                .subscribe { Log.d(TAG, "whitelist item removed from database") }

            whitelistSet.remove(host)
        }
    }

    companion object {
        private const val TAG = "SessionWhitelistModel"
    }
}
