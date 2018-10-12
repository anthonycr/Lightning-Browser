package acr.browser.lightning.dialog

import androidx.annotation.StringRes


/**
 * An item representing a list item in a list dialog. The item has a [title], an [onClick] function
 * to be invoked when the item is clicked, and a boolean condition [isConditionMet] which defaults
 * to true and allows the consumer to control the visibility of the item in the list.
 */
class DialogItem @JvmOverloads constructor(
    @param:StringRes val title: Int,
    val isConditionMet: Boolean = true,
    private val onClick: () -> Unit
) {

    fun onClick() = onClick.invoke()

}
