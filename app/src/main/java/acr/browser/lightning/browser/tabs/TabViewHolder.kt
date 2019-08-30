package acr.browser.lightning.browser.tabs

import acr.browser.lightning.R
import acr.browser.lightning.controller.UIController
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * The [RecyclerView.ViewHolder] for both vertical and horizontal tabs.
 */
class TabViewHolder(
    view: View,
    private val uiController: UIController
) : RecyclerView.ViewHolder(view), View.OnClickListener, View.OnLongClickListener {

    val txtTitle: TextView = view.findViewById(R.id.textTab)
    val favicon: ImageView = view.findViewById(R.id.faviconTab)
    val exitButton: FrameLayout = view.findViewById(R.id.deleteAction)
    val layout: LinearLayout = view.findViewById(R.id.tab_item_background)

    init {
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
