package acr.browser.lightning.browser.bookmark

import acr.browser.lightning.databinding.BookmarkListItemBinding
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * The view holder that shows bookmark items.
 *
 * @param onItemClickListener Invoked when the cell is clicked.
 * @param onItemLongClickListener Invoked when the cell is long pressed.
 */
class BookmarkViewHolder(
    itemView: View,
    private val onItemLongClickListener: (Int) -> Unit,
    private val onItemClickListener: (Int) -> Unit
) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {

    val binding = BookmarkListItemBinding.bind(itemView)

    init {
        itemView.setOnLongClickListener(this)
        itemView.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        onItemClickListener(adapterPosition)
    }

    override fun onLongClick(v: View): Boolean {
        onItemLongClickListener(adapterPosition)
        return true
    }
}
