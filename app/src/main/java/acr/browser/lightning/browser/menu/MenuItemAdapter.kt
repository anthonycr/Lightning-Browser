package acr.browser.lightning.browser.menu

import acr.browser.lightning.R
import android.view.MenuItem
import javax.inject.Inject

/**
 * Adapts a click on a menu item to a [MenuSelection].
 */
class MenuItemAdapter @Inject constructor() {

    /**
     * Adapt the [menuItem] or return null if the item is unsupported.
     */
    fun adaptMenuItem(menuItem: MenuItem): MenuSelection? {
        return when (menuItem.itemId) {
            android.R.id.home -> TODO()
            R.id.action_back -> MenuSelection.BACK
            R.id.action_forward -> MenuSelection.FORWARD
            R.id.action_add_to_homescreen -> MenuSelection.ADD_TO_HOME
            R.id.action_new_tab -> MenuSelection.NEW_TAB
            R.id.action_incognito -> MenuSelection.NEW_INCOGNITO_TAB
            R.id.action_share -> MenuSelection.SHARE
            R.id.action_bookmarks -> MenuSelection.BOOKMARKS
            R.id.action_copy -> MenuSelection.COPY_LINK
            R.id.action_settings -> MenuSelection.SETTINGS
            R.id.action_history -> MenuSelection.HISTORY
            R.id.action_downloads -> MenuSelection.DOWNLOADS
            R.id.action_add_bookmark -> MenuSelection.ADD_BOOKMARK
            R.id.action_find -> MenuSelection.FIND
            R.id.action_reading_mode -> MenuSelection.READER
            else -> null
        }
    }

}
