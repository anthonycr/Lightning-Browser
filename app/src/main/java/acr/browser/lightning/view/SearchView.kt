package acr.browser.lightning.view

import android.content.Context
import android.support.v7.widget.AppCompatAutoCompleteTextView
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration

class SearchView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : AppCompatAutoCompleteTextView(
        context,
        attrs,
        defStyleAttr
) {

    interface PreFocusListener {
        fun onPreFocus()
    }

    private var listener: PreFocusListener? = null
    private var isBeingClicked: Boolean = false
    private var timePressed: Long = 0

    fun setOnPreFocusListener(listener: PreFocusListener?) {
        this.listener = listener
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                timePressed = System.currentTimeMillis()
                isBeingClicked = true
            }
            MotionEvent.ACTION_CANCEL -> isBeingClicked = false
            MotionEvent.ACTION_UP -> if (isBeingClicked && !isLongPress) {
                if (listener != null) {
                    listener!!.onPreFocus()
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private val isLongPress: Boolean
        get() = System.currentTimeMillis() - timePressed >= ViewConfiguration.getLongPressTimeout()


}
