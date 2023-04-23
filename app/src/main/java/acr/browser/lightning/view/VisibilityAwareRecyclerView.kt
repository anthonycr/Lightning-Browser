package acr.browser.lightning.view

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView

/**
 * Created by anthonycr on 3/13/23.
 */
class VisibilityAwareRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defaultStyleRes: Int = 0
) : RecyclerView(context, attrs, defaultStyleRes) {

    var isVisibleListener: ((Boolean) -> Unit)? = null

    override fun setVisibility(visibility: Int) {
        val wasVisible = isVisible
        super.setVisibility(visibility)
        if (wasVisible != isVisible) {
            isVisibleListener?.invoke(isVisible)
        }
    }
}
