package acr.browser.lightning.extensions

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver

/**
 * Removes a view from its parent if it has one.
 */
fun View?.removeFromParent() = this?.let {
    val parent = it.parent
    (parent as? ViewGroup)?.removeView(it)
}

/**
 * Performs an action when the view is laid out.
 *
 * @param runnable the runnable to run when the view is laid out.
 */
fun View?.doOnLayout(runnable: () -> Unit) = this?.let {
    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
            } else {

                viewTreeObserver.removeGlobalOnLayoutListener(this)
            }
            runnable()
        }
    })
}