package acr.browser.lightning.browser.tab

import acr.browser.lightning.R
import acr.browser.lightning.browser.theme.ThemeProvider
import acr.browser.lightning.databinding.TabPreviewItemBinding
import acr.browser.lightning.extensions.drawable
import acr.browser.lightning.extensions.inflater
import acr.browser.lightning.preview.TopCropTransformation
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import coil3.load
import coil3.request.placeholder
import coil3.request.transformations
import java.io.File


/**
 * The adapter that renders tabs in the drawer list form.
 *
 * @param onClick Invoked when the tab is clicked.
 * @param onLongClick Invoked when the tab is long pressed.
 * @param onCloseClick Invoked when the tab's close button is clicked.
 */
class BottomDrawerTabRecyclerViewAdapter(
    private val themeProvider: ThemeProvider,
    private val onClick: (Int) -> Unit,
    private val onLongClick: (Int) -> Unit,
    private val onCloseClick: (Int) -> Unit,
    private val onBackClick: (Int) -> Unit,
    private val onForwardClick: (Int) -> Unit,
    private val onHomeClick: (Int) -> Unit
) : ListAdapter<TabViewState, TabViewHolder>(
    object : DiffUtil.ItemCallback<TabViewState>() {
        override fun areItemsTheSame(oldItem: TabViewState, newItem: TabViewState): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: TabViewState, newItem: TabViewState): Boolean =
            oldItem == newItem
    }
) {

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): TabViewHolder {
        val tabPreviewItemBinding =
            TabPreviewItemBinding.inflate(viewGroup.context.inflater, viewGroup, false)

        return TabViewHolder(
            tabPreviewItemBinding.root,
            onClick = onClick,
            onLongClick = onLongClick,
            onCloseClick = onCloseClick
        ).apply {
            tabPreviewItemBinding.previewImage.setOnClickListener { onClick(bindingAdapterPosition) }
//            tabPreviewItemBinding.actionBack.setOnClickListener { onBackClick(bindingAdapterPosition) }
//            tabPreviewItemBinding.actionForward.setOnClickListener {
//                onForwardClick(
//                    bindingAdapterPosition
//                )
//            }
//            tabPreviewItemBinding.actionHome.setOnClickListener { onHomeClick(bindingAdapterPosition) }
        }
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        holder.exitButton.tag = position

        val tab = getItem(position)

        holder.txtTitle.text = tab.title
        if (tab.isSelected) {
            holder.view.background =
                holder.view.context.drawable(R.drawable.tab_background_selected)
        } else {
            holder.view.background = holder.view.context.drawable(R.drawable.tab_background)
        }

        tab.icon?.let(holder.favicon::setImageBitmap)
            ?: holder.favicon.setImageResource(R.drawable.ic_webpage)
        loadImage(holder.itemView.findViewById(R.id.preview_image), tab)
    }

    private fun loadImage(imageView: ImageView, tab: TabViewState) {
        val url = tab.preview.first ?: return run {
            imageView.load(null)
        }
        imageView.tag = tab.id
        imageView.load(File(url)) {
            transformations(TopCropTransformation)
            memoryCacheKey(tab.preview.first + tab.preview.second.toString())
            placeholder(themeProvider.color(R.attr.colorPrimary).toDrawable())
        }
    }
}
