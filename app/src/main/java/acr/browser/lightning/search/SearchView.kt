package acr.browser.lightning.search

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.appcompat.R
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import java.util.concurrent.TimeUnit

class SearchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.autoCompleteTextViewStyle
) : AppCompatAutoCompleteTextView(context, attrs, defStyleAttr) {

    interface PreFocusListener {
        fun onPreFocus()
    }

    var onPreFocusListener: PreFocusListener? = null
    private var isBeingClicked: Boolean = false
    private var timePressedNs: Long = 0

    init {
        setSelectAllOnFocus(true)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                timePressedNs = System.nanoTime()
                isBeingClicked = true
            }
            MotionEvent.ACTION_CANCEL -> isBeingClicked = false
            MotionEvent.ACTION_UP -> if (isBeingClicked && !isLongPress(timePressedNs)) {
                onPreFocusListener?.onPreFocus()
            }
        }

        return super.onTouchEvent(event)
    }

    private fun isLongPress(actionDownTime: Long): Boolean =
        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - actionDownTime) >= ViewConfiguration.getLongPressTimeout()


}
