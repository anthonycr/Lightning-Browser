package acr.browser.lightning.browser.view.delegates

import acr.browser.lightning.browser.view.ViewDelegate
import acr.browser.lightning.databinding.BrowserActivityBottomBinding
import acr.browser.lightning.icon.TabCountView
import acr.browser.lightning.search.SearchView
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView

class BottomTabViewDelegate(binding: BrowserActivityBottomBinding) : ViewDelegate {
    override val root: CoordinatorLayout = binding.root
    override val toolbar: Toolbar = binding.toolbar
    override val contentFrame: FrameLayout = binding.contentFrame
    override val uiLayout: LinearLayout = binding.uiLayout
    override val browserLayoutContainer: FrameLayout = binding.browserLayoutContainer
    override val toolbarLayout: ConstraintLayout = binding.toolbarLayout
    override val drawerLayout: DrawerLayout = binding.drawerLayout
    override val tabDrawer: LinearLayout = binding.tabDrawer
    override val bookmarkDrawer: LinearLayout = binding.bookmarkDrawer
    override val homeImageView: ImageView = binding.homeImageView
    override val tabCountView: TabCountView = binding.tabCountView
    override val drawerTabsList: RecyclerView = binding.drawerTabsList
    override val desktopTabsList: RecyclerView = binding.desktopTabsList
    override val bookmarkListView: RecyclerView = binding.bookmarkListView
    override val searchContainer: ConstraintLayout = binding.searchContainer
    override val search: SearchView = binding.search
    override val findBar: LinearLayout = binding.findBar
    override val findQuery: TextView = binding.findQuery
    override val findPrevious: ImageButton = binding.findPrevious
    override val findNext: ImageButton = binding.findNext
    override val findQuit: ImageButton = binding.findQuit
    override val homeButton: FrameLayout = binding.homeButton
    override val actionBack: ImageView = binding.actionBack
    override val actionForward: ImageView = binding.actionForward
    override val actionHome: ImageView = binding.actionHome
    override val newTabButton: ImageView = binding.newTabButton
    override val searchRefresh: ImageView = binding.searchRefresh
    override val actionAddBookmark: ImageView = binding.actionAddBookmark
    override val actionPageTools: ImageView = binding.actionPageTools
    override val tabHeaderButton: ImageView = binding.tabHeaderButton
    override val bookmarkBackButton: ImageView = binding.bookmarkBackButton
    override val searchSslStatus: ImageView = binding.searchSslStatus
    override val progressView: ProgressBar = binding.progressView
}
