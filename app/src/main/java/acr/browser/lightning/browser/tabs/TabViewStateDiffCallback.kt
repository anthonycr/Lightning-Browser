package acr.browser.lightning.browser.tabs

import androidx.recyclerview.widget.DiffUtil

/**
 * Diffing callback used to determine whether changes have been made to the list.
 *
 * @param oldList The old list that is being replaced by the [newList].
 * @param newList The new list replacing the [oldList], which may or may not be different.
 */
class TabViewStateDiffCallback(
    private val oldList: List<TabViewState>,
    private val newList: List<TabViewState>
) : DiffUtil.Callback() {
    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        oldList[oldItemPosition].id == newList[newItemPosition].id

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldList[oldItemPosition] == newList[newItemPosition]
}
