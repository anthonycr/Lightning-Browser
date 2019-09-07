package acr.browser.lightning.browser.bookmarks

import acr.browser.lightning.R
import acr.browser.lightning.adblock.allowlist.AllowListModel
import acr.browser.lightning.animation.AnimationUtils
import acr.browser.lightning.browser.BookmarksView
import acr.browser.lightning.browser.TabsManager
import acr.browser.lightning.controller.UIController
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.bookmark.BookmarkRepository
import acr.browser.lightning.di.DatabaseScheduler
import acr.browser.lightning.di.MainScheduler
import acr.browser.lightning.di.NetworkScheduler
import acr.browser.lightning.di.injector
import acr.browser.lightning.dialog.BrowserDialog
import acr.browser.lightning.dialog.DialogItem
import acr.browser.lightning.dialog.LightningDialogBuilder
import acr.browser.lightning.extensions.color
import acr.browser.lightning.extensions.drawable
import acr.browser.lightning.extensions.inflater
import acr.browser.lightning.favicon.FaviconModel
import acr.browser.lightning.reading.activity.ReadingActivity
import acr.browser.lightning.utils.isSpecialUrl
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * The view that displays bookmarks in a list and some controls.
 */
class BookmarksDrawerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), BookmarksView {

    @Inject internal lateinit var bookmarkModel: BookmarkRepository
    @Inject internal lateinit var allowListModel: AllowListModel
    @Inject internal lateinit var bookmarksDialogBuilder: LightningDialogBuilder
    @Inject internal lateinit var faviconModel: FaviconModel
    @Inject @field:DatabaseScheduler internal lateinit var databaseScheduler: Scheduler
    @Inject @field:NetworkScheduler internal lateinit var networkScheduler: Scheduler
    @Inject @field:MainScheduler internal lateinit var mainScheduler: Scheduler

    private val uiController: UIController

    // Adapter
    private var bookmarkAdapter: BookmarkListAdapter? = null

    // Colors
    private var scrollIndex: Int = 0

    private var bookmarksSubscription: Disposable? = null
    private var bookmarkUpdateSubscription: Disposable? = null

    private val uiModel = BookmarkUiModel()

    private var bookmarkRecyclerView: RecyclerView? = null
    private var backNavigationImageView: ImageView? = null
    private var addBookmarkImageView: ImageView? = null
    private var addBookmarkView: View? = null

    init {
        context.inflater.inflate(R.layout.bookmark_drawer, this, true)
        context.injector.inject(this)

        uiController = context as UIController

        bookmarkRecyclerView = findViewById(R.id.bookmark_list_view)
        backNavigationImageView = findViewById(R.id.bookmark_back_button_image)
        addBookmarkImageView = findViewById(R.id.action_add_bookmark_image)
        addBookmarkView = findViewById(R.id.action_add_bookmark)
        findViewById<View>(R.id.bookmark_back_button).setOnClickListener {
            if (!uiModel.isCurrentFolderRoot()) {
                setBookmarksShown(null, true)
                bookmarkRecyclerView?.layoutManager?.scrollToPosition(scrollIndex)
            }
        }
        addBookmarkView?.setOnClickListener { uiController.bookmarkButtonClicked() }
        findViewById<View>(R.id.action_reading).setOnClickListener {
            getTabsManager().currentTab?.url?.let {
                ReadingActivity.launch(context, it)
            }
        }
        findViewById<View>(R.id.action_page_tools).setOnClickListener { showPageToolsDialog(context) }

        bookmarkAdapter = BookmarkListAdapter(
            context,
            faviconModel,
            networkScheduler,
            mainScheduler,
            ::handleItemLongPress,
            ::handleItemClick
        )

        bookmarkRecyclerView?.let {
            it.layoutManager = LinearLayoutManager(context)
            it.adapter = bookmarkAdapter
        }

        setBookmarksShown(null, true)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        bookmarksSubscription?.dispose()
        bookmarkUpdateSubscription?.dispose()

        bookmarkAdapter?.cleanupSubscriptions()
    }

    private fun getTabsManager(): TabsManager = uiController.getTabModel()

    private fun updateBookmarkIndicator(url: String) {
        bookmarkUpdateSubscription?.dispose()
        bookmarkUpdateSubscription = bookmarkModel.isBookmark(url)
            .subscribeOn(databaseScheduler)
            .observeOn(mainScheduler)
            .subscribe { isBookmark ->
                bookmarkUpdateSubscription = null
                addBookmarkImageView?.isSelected = isBookmark
                addBookmarkImageView?.isEnabled = !url.isSpecialUrl()
                addBookmarkView?.isEnabled = !url.isSpecialUrl()
            }
    }

    override fun handleBookmarkDeleted(bookmark: Bookmark) = when (bookmark) {
        is Bookmark.Folder -> setBookmarksShown(null, false)
        is Bookmark.Entry -> bookmarkAdapter?.deleteItem(BookmarksViewModel(bookmark)) ?: Unit
    }

    private fun setBookmarksShown(folder: String?, animate: Boolean) {
        bookmarksSubscription?.dispose()
        bookmarksSubscription = bookmarkModel.getBookmarksFromFolderSorted(folder)
            .concatWith(Single.defer {
                if (folder == null) {
                    bookmarkModel.getFoldersSorted()
                } else {
                    Single.just(emptyList())
                }
            })
            .toList()
            .map { it.flatten() }
            .subscribeOn(databaseScheduler)
            .observeOn(mainScheduler)
            .subscribe { bookmarksAndFolders ->
                uiModel.currentFolder = folder
                setBookmarkDataSet(bookmarksAndFolders, animate)
            }
    }

    private fun setBookmarkDataSet(items: List<Bookmark>, animate: Boolean) {
        bookmarkAdapter?.updateItems(items.map { BookmarksViewModel(it) })
        val resource = if (uiModel.isCurrentFolderRoot()) {
            R.drawable.ic_action_star
        } else {
            R.drawable.ic_action_back
        }

        if (animate) {
            backNavigationImageView?.let {
                val transition = AnimationUtils.createRotationTransitionAnimation(it, resource)
                it.startAnimation(transition)
            }
        } else {
            backNavigationImageView?.setImageResource(resource)
        }
    }

    private fun handleItemLongPress(bookmark: Bookmark): Boolean {
        (context as Activity?)?.let {
            when (bookmark) {
                is Bookmark.Folder -> bookmarksDialogBuilder.showBookmarkFolderLongPressedDialog(it, uiController, bookmark)
                is Bookmark.Entry -> bookmarksDialogBuilder.showLongPressedDialogForBookmarkUrl(it, uiController, bookmark)
            }
        }
        return true
    }

    private fun handleItemClick(bookmark: Bookmark) = when (bookmark) {
        is Bookmark.Folder -> {
            scrollIndex = (bookmarkRecyclerView?.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            setBookmarksShown(bookmark.title, true)
        }
        is Bookmark.Entry -> uiController.bookmarkItemClicked(bookmark)
    }


    /**
     * Show the page tools dialog.
     */
    private fun showPageToolsDialog(context: Context) {
        val currentTab = getTabsManager().currentTab ?: return
        val isAllowedAds = allowListModel.isUrlAllowedAds(currentTab.url)
        val whitelistString = if (isAllowedAds) {
            R.string.dialog_adblock_enable_for_site
        } else {
            R.string.dialog_adblock_disable_for_site
        }

        BrowserDialog.showWithIcons(context, context.getString(R.string.dialog_tools_title),
            DialogItem(
                icon = context.drawable(R.drawable.ic_action_desktop),
                title = R.string.dialog_toggle_desktop
            ) {
                getTabsManager().currentTab?.apply {
                    toggleDesktopUA()
                    reload()
                    // TODO add back drawer closing
                }
            },
            DialogItem(
                icon = context.drawable(R.drawable.ic_block),
                colorTint = context.color(R.color.error_red).takeIf { isAllowedAds },
                title = whitelistString,
                isConditionMet = !currentTab.url.isSpecialUrl()
            ) {
                if (isAllowedAds) {
                    allowListModel.removeUrlFromAllowList(currentTab.url)
                } else {
                    allowListModel.addUrlToAllowList(currentTab.url)
                }
                getTabsManager().currentTab?.reload()
            }
        )
    }

    override fun navigateBack() {
        if (uiModel.isCurrentFolderRoot()) {
            uiController.onBackButtonPressed()
        } else {
            setBookmarksShown(null, true)
            bookmarkRecyclerView?.layoutManager?.scrollToPosition(scrollIndex)
        }
    }

    override fun handleUpdatedUrl(url: String) {
        updateBookmarkIndicator(url)
        val folder = uiModel.currentFolder
        setBookmarksShown(folder, false)
    }

    private class BookmarkViewHolder(
        itemView: View,
        private val adapter: BookmarkListAdapter,
        private val onItemLongClickListener: (Bookmark) -> Boolean,
        private val onItemClickListener: (Bookmark) -> Unit
    ) : RecyclerView.ViewHolder(itemView), OnClickListener, OnLongClickListener {

        val txtTitle: TextView = itemView.findViewById(R.id.textBookmark)
        val favicon: ImageView = itemView.findViewById(R.id.faviconBookmark)

        init {
            itemView.setOnLongClickListener(this)
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            val index = adapterPosition
            if (index.toLong() != RecyclerView.NO_ID) {
                onItemClickListener(adapter.itemAt(index).bookmark)
            }
        }

        override fun onLongClick(v: View): Boolean {
            val index = adapterPosition
            return index != RecyclerView.NO_POSITION && onItemLongClickListener(adapter.itemAt(index).bookmark)
        }
    }

    private class BookmarkListAdapter(
        context: Context,
        private val faviconModel: FaviconModel,
        private val networkScheduler: Scheduler,
        private val mainScheduler: Scheduler,
        private val onItemLongClickListener: (Bookmark) -> Boolean,
        private val onItemClickListener: (Bookmark) -> Unit
    ) : RecyclerView.Adapter<BookmarkViewHolder>() {

        private var bookmarks: List<BookmarksViewModel> = listOf()
        private val faviconFetchSubscriptions = ConcurrentHashMap<String, Disposable>()
        private val folderIcon = context.drawable(R.drawable.ic_folder)
        private val webpageIcon = context.drawable(R.drawable.ic_webpage)

        fun itemAt(position: Int): BookmarksViewModel = bookmarks[position]

        fun deleteItem(item: BookmarksViewModel) {
            val newList = bookmarks - item
            updateItems(newList)
        }

        fun updateItems(newList: List<BookmarksViewModel>) {
            val oldList = bookmarks
            bookmarks = newList

            val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize() = oldList.size

                override fun getNewListSize() = bookmarks.size

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                    oldList[oldItemPosition].bookmark.url == bookmarks[newItemPosition].bookmark.url

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                    oldList[oldItemPosition] == bookmarks[newItemPosition]
            })

            diffResult.dispatchUpdatesTo(this)
        }

        fun cleanupSubscriptions() {
            for (subscription in faviconFetchSubscriptions.values) {
                subscription.dispose()
            }
            faviconFetchSubscriptions.clear()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val itemView = inflater.inflate(R.layout.bookmark_list_item, parent, false)

            return BookmarkViewHolder(itemView, this, onItemLongClickListener, onItemClickListener)
        }

        override fun onBindViewHolder(holder: BookmarkViewHolder, position: Int) {
            holder.itemView.jumpDrawablesToCurrentState()

            val viewModel = bookmarks[position]
            holder.txtTitle.text = viewModel.bookmark.title

            val url = viewModel.bookmark.url
            holder.favicon.tag = url

            viewModel.icon?.let {
                holder.favicon.setImageBitmap(it)
                return
            }

            val imageDrawable = when (viewModel.bookmark) {
                is Bookmark.Folder -> folderIcon
                is Bookmark.Entry -> webpageIcon.also {
                    faviconFetchSubscriptions[url]?.dispose()
                    faviconFetchSubscriptions[url] = faviconModel
                        .faviconForUrl(url, viewModel.bookmark.title)
                        .subscribeOn(networkScheduler)
                        .observeOn(mainScheduler)
                        .subscribeBy(
                            onSuccess = { bitmap ->
                                viewModel.icon = bitmap
                                if (holder.favicon.tag == url) {
                                    holder.favicon.setImageBitmap(bitmap)
                                }
                            }
                        )
                }
            }

            holder.favicon.setImageDrawable(imageDrawable)
        }

        override fun getItemCount() = bookmarks.size
    }

}
