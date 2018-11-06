package acr.browser.lightning.dialog

import android.graphics.drawable.Drawable
import androidx.annotation.StringRes


/**
 * An item representing a list item in a list dialog. The item has an [icon], [title], an [onClick]
 * function to be invoked when the item is clicked, and a boolean condition [isConditionMet] which
 * defaults to true and allows the consumer to control the visibility of the item in the list.
 */
class DialogItem(
    val icon: Drawable? = null,
    @param:StringRes val title: Int,
    val isConditionMet: Boolean = true,
    private val onClick: () -> Unit
) {

    fun onClick() = onClick.invoke()

}
