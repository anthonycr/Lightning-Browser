package acr.browser.lightning.browser.fragment

import acr.browser.lightning.R
import acr.browser.lightning.browser.TabsManager
import acr.browser.lightning.browser.TabsView
import acr.browser.lightning.browser.fragment.anim.HorizontalItemAnimator
import acr.browser.lightning.browser.fragment.anim.VerticalItemAnimator
import acr.browser.lightning.controller.UIController
import acr.browser.lightning.di.injector
import acr.browser.lightning.extensions.color
import acr.browser.lightning.extensions.desaturate
import acr.browser.lightning.extensions.drawTrapezoid
import acr.browser.lightning.preference.UserPreferences
import acr.browser.lightning.utils.DrawableUtils
import acr.browser.lightning.utils.ThemeUtils
import acr.browser.lightning.utils.Utils
import acr.browser.lightning.view.BackgroundDrawable
import acr.browser.lightning.view.LightningView
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.tab_drawer.*
import java.util.*
import javax.inject.Inject

/**
 * A fragment that holds and manages the tabs and interaction with the tabs. It is reliant on the
 * BrowserController in order to get the current UI state of the browser. It also uses the
 * BrowserController to signal that the  the desktop tabs. It delegates touch events for the tab UI
 * appropriately.
 */
class TabsFragment : Fragment(), View.OnClickListener, View.OnLongClickListener, TabsView {

    private var isIncognito: Boolean = false
    private var darkTheme: Boolean = false
    private var iconColor: Int = 0
    private var colorMode = true
    private var showInNavigationDrawer: Boolean = false

    private var tabsAdapter: LightningViewAdapter? = null
    private lateinit var uiController: UIController

    @Inject internal lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(this)
        val context = requireNotNull(context) { "Context should never be null in onCreate" }
        uiController = activity as UIController
        isIncognito = arguments?.getBoolean(IS_INCOGNITO, false) == true
        showInNavigationDrawer = arguments?.getBoolean(VERTICAL_MODE, true) == true
        darkTheme = userPreferences.useTheme != 0 || isIncognito
        colorMode = userPreferences.colorModeEnabled
        colorMode = colorMode and !darkTheme

        iconColor = ThemeUtils.getIconThemeColor(context, darkTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view: View
        val context = inflater.context
        if (showInNavigationDrawer) {
            view = inflater.inflate(R.layout.tab_drawer, container, false)
            setupFrameLayoutButton(view, R.id.tab_header_button, R.id.plusIcon)
            setupFrameLayoutButton(view, R.id.new_tab_button, R.id.icon_plus)
            setupFrameLayoutButton(view, R.id.action_back, R.id.icon_back)
            setupFrameLayoutButton(view, R.id.action_forward, R.id.icon_forward)
            setupFrameLayoutButton(view, R.id.action_home, R.id.icon_home)
        } else {
            view = inflater.inflate(R.layout.tab_strip, container, false)
            view.findViewById<ImageView>(R.id.new_tab_button).apply {
                setColorFilter(context.color(R.color.icon_dark_theme))
                setOnClickListener(this@TabsFragment)
                setOnLongClickListener(this@TabsFragment)
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = if (showInNavigationDrawer) {
            LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        } else {
            LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        }

        val animator = (if (showInNavigationDrawer) {
            VerticalItemAnimator()
        } else {
            HorizontalItemAnimator()
        }).apply {
            supportsChangeAnimations = false
            addDuration = 200
            changeDuration = 0
            removeDuration = 200
            moveDuration = 200
        }

        tabsAdapter = LightningViewAdapter(showInNavigationDrawer)

        tabs_list.apply {
            setLayerType(View.LAYER_TYPE_NONE, null)
            itemAnimator = animator
            this.layoutManager = layoutManager
            adapter = tabsAdapter
            setHasFixedSize(true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tabsAdapter = null
    }

    private fun getTabsManager(): TabsManager = uiController.getTabModel()

    private fun setupFrameLayoutButton(root: View, @IdRes buttonId: Int,
                                       @IdRes imageId: Int) {
        val frameButton = root.findViewById<View>(buttonId)
        val buttonImage = root.findViewById<ImageView>(imageId)
        frameButton.setOnClickListener(this)
        frameButton.setOnLongClickListener(this)
        buttonImage.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
    }

    override fun onResume() {
        super.onResume()
        // Force adapter refresh
        tabsAdapter?.notifyDataSetChanged()
    }

    override fun tabsInitialized() {
        tabsAdapter?.notifyDataSetChanged()
    }

    fun reinitializePreferences() {
        val activity = activity ?: return
        darkTheme = userPreferences.useTheme != 0 || isIncognito
        colorMode = userPreferences.colorModeEnabled
        colorMode = colorMode and !darkTheme
        iconColor = ThemeUtils.getIconThemeColor(activity, darkTheme)
        tabsAdapter?.notifyDataSetChanged()
    }

    override fun onClick(v: View) = when (v.id) {
        R.id.tab_header_button -> uiController.showCloseDialog(getTabsManager().indexOfCurrentTab())
        R.id.new_tab_button -> uiController.newTabButtonClicked()
        R.id.action_back -> uiController.onBackButtonPressed()
        R.id.action_forward -> uiController.onForwardButtonPressed()
        R.id.action_home -> uiController.onHomeButtonPressed()
        else -> {
        }
    }

    override fun onLongClick(v: View): Boolean {
        when (v.id) {
            R.id.new_tab_button -> uiController.newTabButtonLongClicked()
            else -> {
            }
        }
        return true
    }

    override fun tabAdded() {
        tabsAdapter?.let {
            it.showTabs(toViewModels(getTabsManager().allTabs))
            tabs_list.postDelayed({ tabs_list.smoothScrollToPosition(it.itemCount - 1) }, 500)
        }
    }

    override fun tabRemoved(position: Int) {
        tabsAdapter?.showTabs(toViewModels(getTabsManager().allTabs))
    }

    override fun tabChanged(position: Int) {
        tabsAdapter?.showTabs(toViewModels(getTabsManager().allTabs))
    }

    private fun toViewModels(tabs: List<LightningView>) = tabs.map(::TabViewState)

    private inner class LightningViewAdapter(
        private val drawerTabs: Boolean
    ) : RecyclerView.Adapter<LightningViewAdapter.LightningViewHolder>() {

        private val layoutResourceId: Int = if (drawerTabs) R.layout.tab_list_item else R.layout.tab_list_item_horizontal
        private val backgroundTabDrawable: Drawable?
        private val foregroundTabBitmap: Bitmap?
        private var tabList: List<TabViewState> = ArrayList()

        init {

            if (drawerTabs) {
                backgroundTabDrawable = null
                foregroundTabBitmap = null
            } else {
                val context = requireNotNull(context) { "Adapter cannot be initialized when fragment is detached" }

                val backgroundColor = Utils.mixTwoColors(ThemeUtils.getPrimaryColor(context), Color.BLACK, 0.75f)
                val backgroundTabBitmap = Bitmap.createBitmap(Utils.dpToPx(175f), Utils.dpToPx(30f), Bitmap.Config.ARGB_8888).also {
                    Canvas(it).drawTrapezoid(backgroundColor, true)
                }
                backgroundTabDrawable = BitmapDrawable(resources, backgroundTabBitmap)

                val foregroundColor = ThemeUtils.getPrimaryColor(context)
                foregroundTabBitmap = Bitmap.createBitmap(Utils.dpToPx(175f), Utils.dpToPx(30f), Bitmap.Config.ARGB_8888).also {
                    Canvas(it).drawTrapezoid(foregroundColor, false)
                }
            }
        }

        internal fun showTabs(tabs: List<TabViewState>) {
            val oldList = tabList
            tabList = ArrayList(tabs)

            val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize() = oldList.size

                override fun getNewListSize() = tabList.size

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                    oldList[oldItemPosition] == tabList[newItemPosition]

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val oldTab = oldList[oldItemPosition]
                    val newTab = tabList[newItemPosition]

                    return (oldTab.title == newTab.title
                        && oldTab.favicon == newTab.favicon
                        && oldTab.isForegroundTab == newTab.isForegroundTab
                        && oldTab == newTab)
                }
            })

            result.dispatchUpdatesTo(this)
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): LightningViewHolder {
            val inflater = LayoutInflater.from(viewGroup.context)
            val view = inflater.inflate(layoutResourceId, viewGroup, false)
            if (drawerTabs) {
                DrawableUtils.setBackground(view, BackgroundDrawable(view.context))
            }
            return LightningViewHolder(view)
        }

        override fun onBindViewHolder(holder: LightningViewHolder, position: Int) {
            holder.exitButton.tag = position

            holder.exitButton.jumpDrawablesToCurrentState()

            val web = tabList[position]

            updateViewHolderTitle(holder, web.title)
            updateViewHolderAppearance(holder, web.favicon, web.isForegroundTab)
            updateViewHolderFavicon(holder, web.favicon, web.isForegroundTab)
            updateViewHolderBackground(holder, web.isForegroundTab)
        }

        private fun updateViewHolderTitle(viewHolder: LightningViewHolder, title: String) {
            viewHolder.txtTitle.text = title
        }

        private fun updateViewHolderFavicon(viewHolder: LightningViewHolder, favicon: Bitmap, isForeground: Boolean) =
            if (isForeground) {
                viewHolder.favicon.setImageBitmap(favicon)
            } else {
                viewHolder.favicon.setImageBitmap(favicon.desaturate())
            }

        private fun updateViewHolderBackground(viewHolder: LightningViewHolder, isForeground: Boolean) {
            if (drawerTabs) {
                val verticalBackground = viewHolder.layout.background as BackgroundDrawable
                verticalBackground.isCrossFadeEnabled = false
                if (isForeground) {
                    verticalBackground.startTransition(200)
                } else {
                    verticalBackground.reverseTransition(200)
                }
            }
        }

        private fun updateViewHolderAppearance(viewHolder: LightningViewHolder, favicon: Bitmap, isForeground: Boolean) {
            if (isForeground) {
                var foregroundDrawable: Drawable? = null
                if (!drawerTabs) {
                    foregroundDrawable = BitmapDrawable(resources, foregroundTabBitmap)
                    if (!isIncognito && colorMode) {
                        foregroundDrawable.setColorFilter(uiController.getUiColor(), PorterDuff.Mode.SRC_IN)
                    }
                }
                TextViewCompat.setTextAppearance(viewHolder.txtTitle, R.style.boldText)
                if (!drawerTabs) {
                    DrawableUtils.setBackground(viewHolder.layout, foregroundDrawable)
                }
                if (!isIncognito && colorMode) {
                    uiController.changeToolbarBackground(favicon, foregroundDrawable)
                }
            } else {
                TextViewCompat.setTextAppearance(viewHolder.txtTitle, R.style.normalText)
                if (!drawerTabs) {
                    DrawableUtils.setBackground(viewHolder.layout, backgroundTabDrawable)
                }
            }
        }

        override fun getItemCount() = tabList.size

        internal inner class LightningViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener, View.OnLongClickListener {

            val txtTitle: TextView = view.findViewById(R.id.textTab)
            val favicon: ImageView = view.findViewById(R.id.faviconTab)
            val exit: ImageView = view.findViewById(R.id.deleteButton)
            val exitButton: FrameLayout = view.findViewById(R.id.deleteAction)
            val layout: LinearLayout = view.findViewById(R.id.tab_item_background)

            init {
                exit.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
                exitButton.setOnClickListener(this)
                layout.setOnClickListener(this)
                layout.setOnLongClickListener(this)
            }

            override fun onClick(v: View) {
                if (v === exitButton) {
                    uiController.tabCloseClicked(adapterPosition)
                } else if (v === layout) {
                    uiController.tabClicked(adapterPosition)
                }
            }

            override fun onLongClick(v: View): Boolean {
                uiController.showCloseDialog(adapterPosition)
                return true
            }
        }

    }

    companion object {

        @JvmStatic
        fun createTabsFragment(isIncognito: Boolean, showTabsInDrawer: Boolean): TabsFragment {
            return TabsFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(TabsFragment.IS_INCOGNITO, isIncognito)
                    putBoolean(TabsFragment.VERTICAL_MODE, showTabsInDrawer)
                }
            }
        }

        private const val TAG = "TabsFragment"

        private const val DESATURATED = 0.5f

        /**
         * Arguments boolean to tell the fragment it is displayed in the drawner or on the tab strip
         * If true, the fragment is in the left drawner in the strip otherwise.
         */
        private const val VERTICAL_MODE = "$TAG.VERTICAL_MODE"
        private const val IS_INCOGNITO = "$TAG.IS_INCOGNITO"
    }
}
