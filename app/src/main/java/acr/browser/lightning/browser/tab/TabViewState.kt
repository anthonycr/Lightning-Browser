package acr.browser.lightning.browser.tab

import android.graphics.Bitmap

/**
 * The view state for a tab.
 *
 * @param id The tab identifier.
 * @param icon The icon for the current webpage, null if there is none.
 * @param title The title of the current webpage.
 * @param isSelected True if the tab is in the foreground, false if it is in the background.
 */
data class TabViewState(
    val id: Int,
    val icon: Bitmap?,
    val title: String,
    val isSelected: Boolean
)
