package acr.browser.lightning.view

import android.content.Context
import android.support.v7.appcompat.R
import android.support.v7.widget.AppCompatAutoCompleteTextView
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration

class SearchView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = R.attr.autoCompleteTextViewStyle
) : AppCompatAutoCompleteTextView(context, attrs, defStyleAttr) {

    interface PreFocusListener {
        fun onPreFocus()
    }

    var onPreFocusListener: PreFocusListener? = null
    var onRightDrawableClickListener: ((SearchView) -> Unit)? = null
    private var isBeingClicked: Boolean = false
    private var timePressed: Long = 0

    override fun onTouchEvent(event: MotionEvent): Boolean {

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                timePressed = System.currentTimeMillis()
                isBeingClicked = true
            }
            MotionEvent.ACTION_CANCEL -> isBeingClicked = false
            MotionEvent.ACTION_UP -> if (isBeingClicked && !isLongPress(timePressed)) {
                onPreFocusListener?.onPreFocus()
            }
        }

        compoundDrawables[2]
                ?.takeIf { event.x > (width - paddingRight - it.intrinsicWidth) }
                ?.let {
                    if (event.action == MotionEvent.ACTION_UP) {
                        onRightDrawableClickListener?.invoke(this@SearchView)
                    }
                    return true
                }


        return super.onTouchEvent(event)
    }

    private fun isLongPress(actionDownTime: Long): Boolean
            = System.currentTimeMillis() - actionDownTime >= ViewConfiguration.getLongPressTimeout()


}
