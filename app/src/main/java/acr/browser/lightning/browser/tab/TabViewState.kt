package acr.browser.lightning.browser.tab

import android.graphics.Bitmap

/**
 * Created by anthonycr on 9/11/20.
 */
data class TabViewState(
    val id: Int,
    val icon: Bitmap?,
    val title: String,
    val isSelected: Boolean
)
