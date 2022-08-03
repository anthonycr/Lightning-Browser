package acr.browser.lightning.browser.view.targetUrl

/**
 * Created by anthonycr on 12/23/20.
 */
data class LongPress(
    val targetUrl: String?,
    val hitUrl: String?,
    val hitCategory: Category = Category.UNKNOWN
) {
    enum class Category {
        IMAGE,
        LINK,
        UNKNOWN
    }
}
