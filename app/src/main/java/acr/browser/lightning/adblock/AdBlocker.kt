package acr.browser.lightning.adblock

/**
 * The ad blocking interface.
 */
interface AdBlocker {

    /**
     * a method that determines if the given URL is an ad or not. It performs a search of the URL's
     * domain on the blocked domain hash set.
     *
     * @param url the URL to check for being an ad, may be null.
     * @return true if it is an ad, false if it is not an ad.
     */
    fun isAd(url: String?): Boolean

}
