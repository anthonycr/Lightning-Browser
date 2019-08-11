/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.controller

import acr.browser.lightning.browser.TabsManager
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.dialog.LightningDialogBuilder
import acr.browser.lightning.view.LightningView
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Message
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient.CustomViewCallback
import androidx.annotation.ColorInt

/**
 * The monolithic controller that routes events between views in the browser.
 */
interface UIController {

    /**
     * @return the current color of the UI as a color integer.
     */
    @ColorInt
    fun getUiColor(): Int

    /**
     * @return whether or not the UI is currently themed in dark mode. True if it is in dark theme,
     * false otherwise.
     */
    fun getUseDarkTheme(): Boolean

    /**
     * @return the tab model which contains all the tabs presented to the user.
     */
    fun getTabModel(): TabsManager

    /**
     * Notifies the controller of a change in the favicon, indicating that the UI should adapt to
     * the color of the favicon.
     *
     * @param favicon the new favicon
     * @param tabBackground the background of the tab, only used when tabs are not displayed in the
     * drawer.
     */
    fun changeToolbarBackground(favicon: Bitmap?, tabBackground: Drawable?)

    /**
     * Updates the current URL of the page.
     *
     * @param url the current URL.
     * @param isLoading true if the [url] is currently being loaded, false otherwise.
     */
    fun updateUrl(url: String?, isLoading: Boolean)

    /**
     * Update the loading progress of the page.
     *
     * @param progress the loading progress of the page, an integer between 0 and 100.
     */
    fun updateProgress(progress: Int)

    /**
     * Notify the controller that a [url] has been visited.
     *
     * @param title the optional title of the current page being viewed.
     * @param url the URL of the page being viewed.
     */
    fun updateHistory(title: String?, url: String)

    /**
     * Notify the controller that it should open the file chooser with the provided callback.
     */
    fun openFileChooser(uploadMsg: ValueCallback<Uri>)

    /**
     * Notify the controller that it should open the file chooser with the provided callback.
     */
    fun showFileChooser(filePathCallback: ValueCallback<Array<Uri>>)

    /**
     * Notify the controller that it should display the custom [view] in full-screen to the user.
     */
    fun onShowCustomView(view: View, callback: CustomViewCallback)

    /**
     * Notify the controller that it should display the custom [view] in full-screen to the user.
     */
    fun onShowCustomView(view: View, callback: CustomViewCallback, requestedOrientation: Int)

    /**
     * Notify the controller that it should hide the custom view which was previously displayed in
     * full screen to the user.
     */
    fun onHideCustomView()

    /**
     * Called when a website wants to open a link in a new window.
     *
     * @param resultMsg the message to send to the new web view that is created.
     */
    fun onCreateWindow(resultMsg: Message)

    /**
     * Notify the browser that the website currently being displayed by the [tab] wants to be
     * closed.
     */
    fun onCloseWindow(tab: LightningView)

    /**
     * Hide the search bar from view via animation.
     */
    fun hideActionBar()

    /**
     * Show the search bar via animation.
     */
    fun showActionBar()

    /**
     * Show the close browser dialog for the tab at [position].
     */
    fun showCloseDialog(position: Int)

    /**
     * Notify the browser that the new tab button was clicked.
     */
    fun newTabButtonClicked()

    /**
     * Notify the browser that the new tab button was long pressed by the user.
     */
    fun newTabButtonLongClicked()

    /**
     * Notify the browser that the tab close button was clicked for the tab at [position].
     */
    fun tabCloseClicked(position: Int)

    /**
     * Notify the browser that the tab at [position] was selected by the user for display.
     */
    fun tabClicked(position: Int)

    /**
     * Notify the browser that the user pressed the bookmark button.
     */
    fun bookmarkButtonClicked()

    /**
     * Notify the browser that the user clicked on the bookmark [entry].
     */
    fun bookmarkItemClicked(entry: Bookmark.Entry)

    /**
     * Notify the UI that the forward button should be [enabled] for interaction.
     */
    fun setForwardButtonEnabled(enabled: Boolean)

    /**
     * Notify the UI that the back button should be [enabled] for interaction.
     */
    fun setBackButtonEnabled(enabled: Boolean)

    /**
     * Notify the UI that the [tab] should be displayed.
     */
    fun tabChanged(tab: LightningView)

    /**
     * Notify the browser that the user pressed the back button.
     */
    fun onBackButtonPressed()

    /**
     * Notify the browser that the user pressed the forward button.
     */
    fun onForwardButtonPressed()

    /**
     * Notify the browser that the user pressed the home (not device home) button.
     */
    fun onHomeButtonPressed()

    /**
     * Notify the browser that the bookmarks list has changed.
     */
    fun handleBookmarksChange()

    /**
     * Notify the browser that the provided bookmark [bookmark] has changed.
     */
    fun handleBookmarkDeleted(bookmark: Bookmark)

    /**
     * Notify the browser that the download list has changed.
     */
    fun handleDownloadDeleted()

    /**
     * Notify the browser that the history list has changed.
     */
    fun handleHistoryChange()

    /**
     * Notify the controller that a new tab action has originated from a dialog with the [url] and
     * the provided [newTabType].
     */
    fun handleNewTab(newTabType: LightningDialogBuilder.NewTab, url: String)


}
