package acr.browser.lightning.view

import acr.browser.lightning.R
import acr.browser.lightning.extensions.pad
import acr.browser.lightning.utils.ThemeUtils
import android.content.Context
import android.graphics.Bitmap

/**
 * [LightningViewTitle] acts as a container class
 * for the favicon and page title, the information used
 * by the tab adapters to show the tabs to the user.
 */
class LightningViewTitle(private val context: Context) {

    private var favicon: Bitmap? = null
    private var title = context.getString(R.string.action_new_tab)

    /**
     * Set the current favicon to a new Bitmap.
     * May be null, if null, the default will be used.
     *
     * @param favicon the potentially null favicon to set.
     */
    fun setFavicon(favicon: Bitmap?) {
        this.favicon = favicon?.pad()
    }

    /**
     * Gets the current title, which is not null. Can be an empty string.
     *
     * @return the non-null title.
     */
    fun getTitle(): String? = title

    /**
     * Set the current title to a new title. If the title is null, an empty title will be used.
     *
     * @param title the title to set.
     */
    fun setTitle(title: String?) {
        this.title = title ?: ""
    }

    /**
     * Gets the favicon of the page, which is not null.
     * Either the favicon, or a default icon.
     *
     * @return the favicon or a default if that is null.
     */
    fun getFavicon(darkTheme: Boolean): Bitmap = favicon ?: getDefaultIcon(context, darkTheme)


    companion object {

        private var defaultDarkIcon: Bitmap? = null
        private var defaultLightIcon: Bitmap? = null

        /**
         * Helper method to initialize the DEFAULT_ICON variables
         *
         * @param context   the context needed to initialize the Bitmap.
         * @param darkTheme whether the icon should be themed dark or not.
         * @return a not null icon.
         */
        private fun getDefaultIcon(context: Context, darkTheme: Boolean): Bitmap = if (darkTheme) {
            var darkIcon = defaultDarkIcon

            if (darkIcon == null) {
                darkIcon = ThemeUtils.getThemedBitmap(context, R.drawable.ic_webpage, true)
                defaultDarkIcon = darkIcon
            }

            darkIcon
        } else {
            var lightIcon = defaultLightIcon

            if (lightIcon == null) {
                lightIcon = ThemeUtils.getThemedBitmap(context, R.drawable.ic_webpage, false)
                defaultLightIcon = lightIcon
            }

            lightIcon
        }
    }

}
