package acr.browser.lightning.browser.tab

import acr.browser.lightning.R
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * The view holder for a tab.
 *
 * @param onClick Invoked when the tab is clicked.
 * @param onLongClick Invoked when the tab is long pressed.
 * @param onCloseClick Invoked when the tab's close button is clicked.
 */
class TabViewHolder(
    view: View,
    private val onClick: (Int) -> Unit,
    private val onLongClick: (Int) -> Unit,
    private val onCloseClick: (Int) -> Unit,
) : RecyclerView.ViewHolder(view), View.OnClickListener, View.OnLongClickListener {

    val txtTitle: TextView = view.findViewById(R.id.textTab)
    val favicon: ImageView = view.findViewById(R.id.faviconTab)
    val exitButton: View = view.findViewById(R.id.deleteAction)
    val layout: LinearLayout = view.findViewById(R.id.tab_item_background)

    init {
        exitButton.setOnClickListener(this)
        layout.setOnClickListener(this)
        layout.setOnLongClickListener(this)
    }

    override fun onClick(v: View) {
        if (v === exitButton) {
            onCloseClick.invoke(adapterPosition)
        } else if (v === layout) {
            onClick.invoke(adapterPosition)
        }
    }

    override fun onLongClick(v: View): Boolean {
        onLongClick.invoke(adapterPosition)
        return true
    }
}
