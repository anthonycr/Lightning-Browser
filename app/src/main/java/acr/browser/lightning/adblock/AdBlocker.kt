package acr.browser.lightning.adblock

import android.net.Uri

/**
 * The ad blocking interface.
 */
interface AdBlocker {

    /**
     * a method that determines if the given URL is an ad or not. It performs a search of the URL's
     * domain on the blocked domain hash set.
     *
     * @param uri the URI to check for being an ad.
     * @return true if it is an ad, false if it is not an ad.
     */
    fun isAd(uri: Uri): Boolean

}
