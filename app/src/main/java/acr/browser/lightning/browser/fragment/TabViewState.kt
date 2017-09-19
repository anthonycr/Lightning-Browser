package acr.browser.lightning.browser.fragment

import acr.browser.lightning.view.LightningView
import android.graphics.Bitmap

/**
 * A view model representing the visual state of a tab.
 */
internal class TabViewState(private val lightningView: LightningView) {

    val title: String = lightningView.title
    val favicon: Bitmap = lightningView.favicon
    val isForegroundTab = lightningView.isForegroundTab

    override fun equals(other: Any?): Boolean =
            other is TabViewState && other.lightningView == lightningView

    override fun hashCode(): Int {
        var result = lightningView.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + favicon.hashCode()
        result = 31 * result + isForegroundTab.hashCode()
        return result
    }

}
