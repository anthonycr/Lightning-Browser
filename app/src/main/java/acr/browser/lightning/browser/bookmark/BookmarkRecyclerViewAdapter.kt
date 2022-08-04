package acr.browser.lightning.browser.bookmark

import acr.browser.lightning.R
import acr.browser.lightning.browser.image.ImageLoader
import acr.browser.lightning.database.Bookmark
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

/**
 * An adapter that creates the views for bookmark list items and binds the bookmark data to them.
 *
 * @param onClick Invoked when the cell is clicked.
 * @param onLongClick Invoked when the cell is long pressed.
 * @param imageLoader The image loader needed to load favicons.
 */
class BookmarkRecyclerViewAdapter(
    private val onClick: (Int) -> Unit,
    private val onLongClick: (Int) -> Unit,
    private val imageLoader: ImageLoader
) : ListAdapter<Bookmark, BookmarkViewHolder>(
    object : DiffUtil.ItemCallback<Bookmark>() {
        override fun areItemsTheSame(oldItem: Bookmark, newItem: Bookmark): Boolean =
            oldItem == newItem

        override fun areContentsTheSame(oldItem: Bookmark, newItem: Bookmark): Boolean =
            oldItem == newItem
    }
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.bookmark_list_item, parent, false)

        return BookmarkViewHolder(
            itemView,
            onItemLongClickListener = onLongClick,
            onItemClickListener = onClick
        )
    }

    override fun onBindViewHolder(holder: BookmarkViewHolder, position: Int) {
        val viewModel = getItem(position)
        holder.binding.textBookmark.text = viewModel.title

        imageLoader.loadImage(holder.binding.faviconBookmark, viewModel)
    }
}
