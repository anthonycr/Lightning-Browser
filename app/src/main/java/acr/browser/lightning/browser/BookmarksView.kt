package acr.browser.lightning.browser

import acr.browser.lightning.database.Bookmark

interface BookmarksView {

    fun navigateBack()

    fun handleUpdatedUrl(url: String)

    fun handleBookmarkDeleted(item: Bookmark)

}
