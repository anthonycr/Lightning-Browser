package acr.browser.lightning.browser

import acr.browser.lightning.database.HistoryItem

interface BookmarksView {

    fun navigateBack()

    fun handleUpdatedUrl(url: String)

    fun handleBookmarkDeleted(item: HistoryItem)

}
