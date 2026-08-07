package acr.browser.lightning.browser.tab

import acr.browser.lightning.tab.TabModel

/**
 * The view state for a tab.
 *
 * @param id The tab identifier.
 * @param icon The icon for the current webpage.
 * @param title The title of the current webpage.
 * @param isSelected True if the tab is in the foreground, false if it is in the background.
 * @param preview The preview of the current webpage.
 */
data class TabViewState(
    val id: Int,
    val icon: TabModel.Favicon,
    val title: String,
    val isSelected: Boolean,
    val preview: TabModel.Preview
)
