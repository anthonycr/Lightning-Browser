package acr.browser.lightning.browser.tab


import acr.browser.lightning.R
import acr.browser.lightning.databinding.TabPreviewItemBinding
import acr.browser.lightning.extensions.inflater
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.widget.ImageView
import androidx.collection.LruCache
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlin.math.roundToInt


/**
 * The adapter that renders tabs in the drawer list form.
 *
 * @param onClick Invoked when the tab is clicked.
 * @param onLongClick Invoked when the tab is long pressed.
 * @param onCloseClick Invoked when the tab's close button is clicked.
 */
class VerticalTabRecyclerViewAdapter(
    private val activity: Activity,
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
    private val lruCache = LruCache<Int, Bitmap>(5)
    private val map = mutableMapOf<Int, Disposable>()

    override fun getItemViewType(position: Int): Int {
        if (itemCount == 1) {
            return ONLY
        }
        if (position == 0) {
            return FIRST
        } else if (position == itemCount - 1) {
            return LAST
        }
        return MIDDLE
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): TabViewHolder {
        val tabPreviewItemBinding =
            TabPreviewItemBinding.inflate(viewGroup.context.inflater, viewGroup, false)
        val params = tabPreviewItemBinding.root.layoutParams as RecyclerView.LayoutParams
        val ratio = when (activity.resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> 1f
            Configuration.ORIENTATION_PORTRAIT -> viewGroup.measuredWidth.toFloat() / viewGroup.measuredHeight
            else -> error("Unsupported orientation")
        }
        val actualWidth = (viewGroup.height * 0.6f).roundToInt() * ratio
        params.marginEnd = when (i) {
            FIRST, MIDDLE -> (viewGroup.measuredWidth * 0.05f).roundToInt()
            ONLY, LAST -> ((viewGroup.measuredWidth - actualWidth) / 2f).roundToInt()
            else -> error("Unexpected type: $i")
        }
        params.marginStart = when (i) {
            ONLY, FIRST -> ((viewGroup.measuredWidth - actualWidth) / 2f).roundToInt()
            MIDDLE, LAST -> (viewGroup.measuredWidth * 0.05f).roundToInt()
            else -> error("Unexpected type: $i")
        }
        tabPreviewItemBinding.root.layoutParams = params

        tabPreviewItemBinding.previewImage.layoutParams =
            (tabPreviewItemBinding.previewImage.layoutParams as ConstraintLayout.LayoutParams).apply {
                dimensionRatio = when (activity.resources.configuration.orientation) {
                    Configuration.ORIENTATION_LANDSCAPE -> "1:1"
                    Configuration.ORIENTATION_PORTRAIT -> "${viewGroup.measuredWidth}:${viewGroup.measuredHeight}"
                    else -> error("Unsupported orientation")
                }
                height = (viewGroup.height * 0.6f).roundToInt()
                verticalChainStyle = ConstraintLayout.LayoutParams.CHAIN_PACKED
            }




        return TabViewHolder(
            tabPreviewItemBinding.root,
            onClick = onClick,
            onLongClick = onLongClick,
            onCloseClick = onCloseClick
        ).apply {
            tabPreviewItemBinding.previewImage.setOnClickListener { onClick(bindingAdapterPosition) }
            tabPreviewItemBinding.bottomHandle.setOnClickListener { onClick(bindingAdapterPosition) }
            tabPreviewItemBinding.actionBack.setOnClickListener { onBackClick(bindingAdapterPosition) }
            tabPreviewItemBinding.actionForward.setOnClickListener {
                onForwardClick(
                    bindingAdapterPosition
                )
            }
            tabPreviewItemBinding.actionHome.setOnClickListener { onHomeClick(bindingAdapterPosition) }
        }
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        holder.exitButton.tag = position

        val tab = getItem(position)

        holder.txtTitle.text = tab.title

        tab.icon?.let(holder.favicon::setImageBitmap)
            ?: holder.favicon.setImageResource(R.drawable.ic_webpage)
        loadImage(holder.itemView.findViewById(R.id.preview_image), tab)
    }

    private fun loadImage(imageView: ImageView, tab: TabViewState) {
        imageView.tag = tab.id
        lruCache[tab.id]?.let(imageView::setImageBitmap) ?: run {
            imageView.setImageDrawable(ColorDrawable(Color.BLACK))
        }
        if (map[tab.id] != null || !tab.isPreviewInvalid) {
            return
        }
        map[tab.id] = Maybe.fromCallable(tab.preview)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = {
                    lruCache.put(tab.id, it)
                    map.remove(tab.id)
                    if (imageView.tag == tab.id) {
                        imageView.setImageBitmap(it)
                    }
                },
                onComplete = {
                    map.remove(tab.id)
                }
            )
    }

    companion object {
        const val FIRST = 1
        const val LAST = 3
        const val ONLY = 0
        const val MIDDLE = 2
    }

}
