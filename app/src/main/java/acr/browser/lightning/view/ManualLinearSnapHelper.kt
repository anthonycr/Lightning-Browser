package acr.browser.lightning.view

import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView

/**
 * Created by anthonycr on 3/13/23.
 */
class ManualLinearSnapHelper {

    private val linearSnapHelper = LinearSnapHelper()
    private var recyclerView: RecyclerView? = null

    fun attachToRecyclerView(recyclerView: RecyclerView) {
        linearSnapHelper.attachToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    fun snapToCurrentPosition() {
        linearSnapHelper.attachToRecyclerView(null)
        this.recyclerView?.let(::attachToRecyclerView)
    }
}
