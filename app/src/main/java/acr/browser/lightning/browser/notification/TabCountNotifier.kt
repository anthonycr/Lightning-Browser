package acr.browser.lightning.browser.notification

/**
 * Notify the browser outside of the regular view that the tab count has changed.
 */
interface TabCountNotifier {

    /**
     * The open tab count has changed to the new [total].
     */
    fun notifyTabCountChange(total: Int)

}
