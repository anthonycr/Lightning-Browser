package acr.browser.lightning.extensions

import android.view.View
import android.view.ViewGroup

/**
 * Removes a view from its parent if it has one.
 */
fun View?.removeFromParent() = this?.let {
    val parent = it.parent
    (parent as? ViewGroup)?.removeView(it)
}
