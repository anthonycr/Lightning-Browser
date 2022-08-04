package acr.browser.lightning.browser.view.targetUrl

/**
 * Represents a long press event by the user on a webpage.
 *
 * @param targetUrl The URL that the browser thinks the user is long pressing on (e.g. when the user
 * presses on an image button, this would be the URL that clicking on the button navigates to).
 * @param hitUrl The URL of the element that user user is directly long pressing on (e.g. when the
 * user presses on an image button, this would be the image URL).
 * @param hitCategory The type of URL that is being pressed on.
 */
data class LongPress(
    val targetUrl: String?,
    val hitUrl: String?,
    val hitCategory: Category = Category.UNKNOWN
) {
    /**
     * The category of the long press.
     */
    enum class Category {
        IMAGE,
        LINK,
        UNKNOWN
    }
}
