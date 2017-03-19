package acr.browser.lightning.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.AutoCompleteTextView;

public class SearchView extends AutoCompleteTextView {

    public interface PreFocusListener {
        void onPreFocus();
    }

    @Nullable private PreFocusListener mListener;
    private boolean mIsBeingClicked;
    private long mTimePressed;

    public SearchView(Context context) {
        super(context);
    }

    public SearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SearchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnPreFocusListener(@Nullable PreFocusListener listener) {
        mListener = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTimePressed = System.currentTimeMillis();
                mIsBeingClicked = true;
                break;
            case MotionEvent.ACTION_CANCEL:
                mIsBeingClicked = false;
                break;
            case MotionEvent.ACTION_UP:
                if (mIsBeingClicked && !isLongPress()) {
                    if (mListener != null) {
                        mListener.onPreFocus();
                    }
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    private boolean isLongPress() {
        return (System.currentTimeMillis() - mTimePressed) >= ViewConfiguration.getLongPressTimeout();
    }


}
